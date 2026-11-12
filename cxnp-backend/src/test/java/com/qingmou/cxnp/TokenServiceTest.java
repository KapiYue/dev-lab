
package com.qingmou.cxnp;

import com.qingmou.cxnp.entity.SysUser;
import com.qingmou.cxnp.mapper.SysUserMapper;
import com.qingmou.cxnp.service.TokenService;
import com.qingmou.cxnp.service.impl.TokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TokenServiceTest {

    private final Map<String, String> tokenStore = new HashMap<>();

    private TokenService tokenService;
    private SysUserMapper sysUserMapper;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        sysUserMapper = mock(SysUserMapper.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
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
}
