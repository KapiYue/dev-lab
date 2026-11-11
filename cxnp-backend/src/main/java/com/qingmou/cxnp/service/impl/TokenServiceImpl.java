
package com.qingmou.cxnp.service.impl;

import com.qingmou.cxnp.entity.SysUser;
import com.qingmou.cxnp.mapper.SysUserMapper;
import com.qingmou.cxnp.service.TokenService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;

@Service
public class TokenServiceImpl implements TokenService {

    private static final String TOKEN_PREFIX = "login:token:";
    private static final Duration TOKEN_TTL = Duration.ofHours(2);

    private final StringRedisTemplate redisTemplate;
    private final SysUserMapper sysUserMapper;

    private final SecureRandom secureRandom = new SecureRandom();

    public TokenServiceImpl(
            StringRedisTemplate redisTemplate,
            SysUserMapper sysUserMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public String createToken(Long userId) {

        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException(
                    "用户 ID 不能为空且必须大于 0"
            );
        }

        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);

        String token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        String key = TOKEN_PREFIX + token;

        redisTemplate.opsForValue().set(
                key,
                String.valueOf(userId),
                TOKEN_TTL
        );

        return token;
    }

    @Override
    public Long verifyToken(String token) {

        // 1. 检查 Token 参数
        if (token == null || token.isBlank()) {
            return null;
        }

        // 2. 从 Redis 查询 Token 对应的用户 ID
        String storedUserId = redisTemplate.opsForValue().get(
                TOKEN_PREFIX + token
        );

        if (storedUserId == null) {
            return null;
        }

        // 3. 解析用户 ID
        Long userId;

        try {
            userId = Long.valueOf(storedUserId);
        } catch (NumberFormatException e) {
            return null;
        }

        if (userId <= 0) {
            return null;
        }

        // 4. 查询 MySQL 中的真实用户账号
        SysUser user = sysUserMapper.selectById(userId);

        // 5. 用户不存在或账号未启用，拒绝 Token
        if (user == null
                || !Objects.equals(user.getStatus(), 1)) {
            return null;
        }

        // 6. Token 有效且账号启用
        return userId;
    }
}
