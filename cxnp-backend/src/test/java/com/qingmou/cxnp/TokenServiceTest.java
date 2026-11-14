
package com.qingmou.cxnp;

import com.qingmou.cxnp.entity.SysUser;
import com.qingmou.cxnp.mapper.SysUserMapper;
import com.qingmou.cxnp.service.TokenService;
import com.qingmou.cxnp.service.impl.TokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenServiceTest {

    private final Map<String, String> tokenStore = new HashMap<>();

    private TokenService tokenService;
    private SysUserMapper sysUserMapper;
    private StringRedisTemplate redisTemplate;
    private SetOperations<String, String> setOperations;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        setOperations = mock(SetOperations.class);
        sysUserMapper = mock(SysUserMapper.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.getExpire(anyString(), org.mockito.ArgumentMatchers.eq(TimeUnit.SECONDS)))
                .thenReturn(0L);
        doAnswer(invocation -> {
            tokenStore.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(valueOperations).set(anyString(), anyString(), org.mockito.ArgumentMatchers.any(Duration.class));
        when(valueOperations.get(anyString())).thenAnswer(invocation -> tokenStore.get(invocation.getArgument(0)));

        tokenService = new TokenServiceImpl(redisTemplate, sysUserMapper);
    }

    // 测试1：有效 Token
    @Test
    void testValidToken() {
        SysUser enabledUser = new SysUser();
        enabledUser.setId(1L);
        enabledUser.setStatus(1);
        when(sysUserMapper.selectById(1L)).thenReturn(enabledUser);

        String token = tokenService.createToken(1L);

        assertNotNull(token);
        assertFalse(token.isBlank());

        Long userId = tokenService.verifyToken(token);

        assertEquals(1L, userId);
    }

    // 测试2：无效 Token
    @Test
    void testInvalidToken() {
        Long userId = tokenService.verifyToken("invalid-token");

        assertNull(userId);
    }

    // 测试3：空 Token
    @Test
    void testEmptyToken() {
        assertNull(tokenService.verifyToken(""));
        assertNull(tokenService.verifyToken(null));
    }

    // 测试4：生成不同的 Token
    @Test
    void testUniqueToken() {
        String token1 = tokenService.createToken(1L);
        String token2 = tokenService.createToken(1L);

        assertNotEquals(token1, token2);
    }

    @Test
    void createsIndependentSessionsAndIndexesBothTokens() {
        String token1 = tokenService.createToken(1L);
        String token2 = tokenService.createToken(1L);

        assertNotEquals(token1, token2);
        verify(setOperations, times(2)).add(
                org.mockito.ArgumentMatchers.eq("login:user-tokens:1"),
                org.mockito.ArgumentMatchers.anyString()
        );
        verify(redisTemplate, times(2)).expire(
                "login:user-tokens:1",
                Duration.ofHours(2)
        );
    }

    @Test
    void renewsTokenAndUserIndexWhenSessionIsNearExpiry() {
        SysUser enabledUser = enabledUser(1L);
        when(sysUserMapper.selectById(1L)).thenReturn(enabledUser);
        tokenStore.put("login:token:near-expiry", "1");
        when(redisTemplate.getExpire("login:token:near-expiry", TimeUnit.SECONDS)).thenReturn(30L);

        assertEquals(1L, tokenService.verifyToken("near-expiry"));

        verify(redisTemplate).expire("login:token:near-expiry", Duration.ofHours(2));
        verify(setOperations).add("login:user-tokens:1", "near-expiry");
        verify(redisTemplate).expire("login:user-tokens:1", Duration.ofHours(2));
    }

    @Test
    void rejectsDisabledAccountAndRevokesItsCurrentSession() {
        SysUser disabledUser = enabledUser(1L);
        disabledUser.setStatus(0);
        when(sysUserMapper.selectById(1L)).thenReturn(disabledUser);
        when(setOperations.size("login:user-tokens:1")).thenReturn(0L);
        tokenStore.put("login:token:disabled-user", "1");

        assertNull(tokenService.verifyToken("disabled-user"));

        verify(redisTemplate).delete("login:token:disabled-user");
        verify(setOperations).remove("login:user-tokens:1", "disabled-user");
        verify(redisTemplate).delete("login:user-tokens:1");
    }

    @Test
    void verifiesLegacyTokenAndBuildsUserTokenIndex() {
        SysUser enabledUser = enabledUser(1L);
        when(sysUserMapper.selectById(1L)).thenReturn(enabledUser);
        tokenStore.put("login:token:legacy", "1");
        when(redisTemplate.getExpire("login:token:legacy", TimeUnit.SECONDS)).thenReturn(3_600L);

        assertEquals(1L, tokenService.verifyToken("legacy"));

        verify(setOperations).add("login:user-tokens:1", "legacy");
        verify(redisTemplate).expire("login:user-tokens:1", Duration.ofSeconds(3_600));
        verify(redisTemplate, never()).expire("login:token:legacy", Duration.ofHours(2));
    }

    private SysUser enabledUser(Long userId) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setStatus(1);
        return user;
    }
}
