package com.qingmou.cxnp;

import com.qingmou.cxnp.dto.LoginRequest;
import com.qingmou.cxnp.entity.SysUser;
import com.qingmou.cxnp.exception.AuthenticationException;
import com.qingmou.cxnp.exception.LoginTemporarilyLockedException;
import com.qingmou.cxnp.mapper.SysUserMapper;
import com.qingmou.cxnp.service.TokenService;
import com.qingmou.cxnp.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {

    private SysUserMapper sysUserMapper;
    private PasswordEncoder passwordEncoder;
    private TokenService tokenService;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private AuthServiceImpl authService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        sysUserMapper = mock(SysUserMapper.class);
        passwordEncoder = mock(PasswordEncoder.class);
        tokenService = mock(TokenService.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        authService = new AuthServiceImpl(sysUserMapper, passwordEncoder, tokenService, redisTemplate);
    }

    @Test
    void fifthFailureTemporarilyLocksNormalizedLoginIdentifier() {
        when(redisTemplate.hasKey("login:lock:alice")).thenReturn(false);
        when(sysUserMapper.selectByUsername("alice")).thenReturn(enabledUser());
        when(passwordEncoder.matches("bad-password", "encoded-password")).thenReturn(false);
        when(valueOperations.increment("login:failure:alice"))
                .thenReturn(1L, 2L, 3L, 4L, 5L);

        for (int attempt = 0; attempt < 5; attempt++) {
            assertThrows(AuthenticationException.class,
                    () -> authService.login(loginRequest(" Alice ", "bad-password")));
        }

        verify(redisTemplate).expire("login:failure:alice", Duration.ofMinutes(15));
        verify(redisTemplate).delete("login:failure:alice");
        verify(valueOperations).set("login:lock:alice", "1", Duration.ofMinutes(15));
    }

    @Test
    void lockedLoginDoesNotCheckPassword() {
        when(redisTemplate.hasKey("login:lock:alice")).thenReturn(true);

        assertThrows(LoginTemporarilyLockedException.class,
                () -> authService.login(loginRequest("alice", "password")));

        verify(sysUserMapper, never()).selectByUsername(anyString());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void successfulLoginClearsFailureState() {
        when(redisTemplate.hasKey("login:lock:alice")).thenReturn(false);
        when(sysUserMapper.selectByUsername("alice")).thenReturn(enabledUser());
        when(passwordEncoder.matches("correct-password", "encoded-password")).thenReturn(true);
        when(tokenService.createToken(7L)).thenReturn("token");

        assertEquals("token", authService.login(loginRequest("ALICE", "correct-password")));

        verify(redisTemplate).delete(java.util.List.of("login:failure:alice", "login:lock:alice"));
    }

    @Test
    void loginRecoversAfterLockExpires() {
        when(redisTemplate.hasKey("login:lock:alice")).thenReturn(true, false);
        when(sysUserMapper.selectByUsername("alice")).thenReturn(enabledUser());
        when(passwordEncoder.matches("correct-password", "encoded-password")).thenReturn(true);
        when(tokenService.createToken(7L)).thenReturn("token");

        assertThrows(LoginTemporarilyLockedException.class,
                () -> authService.login(loginRequest("alice", "correct-password")));
        assertEquals("token", authService.login(loginRequest("alice", "correct-password")));

        verify(passwordEncoder, times(1)).matches("correct-password", "encoded-password");
    }

    private LoginRequest loginRequest(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    private SysUser enabledUser() {
        SysUser user = new SysUser();
        user.setId(7L);
        user.setPassword("encoded-password");
        user.setStatus(1);
        return user;
    }
}
