
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
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class TokenServiceImpl implements TokenService {

    private static final String TOKEN_PREFIX = "login:token:";
    private static final String USER_TOKENS_PREFIX = "login:user-tokens:";
    private static final Duration TOKEN_TTL = Duration.ofHours(2);
    private static final Duration RENEWAL_THRESHOLD = Duration.ofMinutes(30);

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
        addToUserTokenIndex(userId, token, TOKEN_TTL);

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
            redisTemplate.delete(TOKEN_PREFIX + token);
            return null;
        }

        if (userId <= 0) {
            redisTemplate.delete(TOKEN_PREFIX + token);
            return null;
        }

        // 4. 查询 MySQL 中的真实用户账号
        SysUser user = sysUserMapper.selectById(userId);

        // 5. 用户不存在或账号未启用，拒绝 Token
        if (user == null
                || !Objects.equals(user.getStatus(), 1)) {
            revokeToken(token);
            return null;
        }

        // 6. 兼容仅有单 Token 键的旧会话，并在临近过期时续期。
        Long remainingSeconds = redisTemplate.getExpire(
                TOKEN_PREFIX + token,
                TimeUnit.SECONDS
        );

        Duration indexTtl = durationOf(remainingSeconds);
        if (remainingSeconds != null
                && remainingSeconds > 0
                && remainingSeconds <= RENEWAL_THRESHOLD.toSeconds()) {
            redisTemplate.expire(TOKEN_PREFIX + token, TOKEN_TTL);
            indexTtl = TOKEN_TTL;
        }

        if (indexTtl != null) {
            addToUserTokenIndex(userId, token, indexTtl);
        }

        // 7. Token 有效且账号启用
        return userId;
    }

    @Override
    public void revokeToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        String tokenKey = TOKEN_PREFIX + token;
        String storedUserId = redisTemplate.opsForValue().get(tokenKey);
        redisTemplate.delete(tokenKey);

        Long userId = parseUserId(storedUserId);
        if (userId == null) {
            return;
        }

        String userTokensKey = USER_TOKENS_PREFIX + userId;
        redisTemplate.opsForSet().remove(userTokensKey, token);
        Long remainingTokens = redisTemplate.opsForSet().size(userTokensKey);
        if (remainingTokens != null && remainingTokens == 0) {
            redisTemplate.delete(userTokensKey);
        }
    }

    @Override
    public void revokeAllUserTokens(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户 ID 不能为空且必须大于 0");
        }

        String userTokensKey = USER_TOKENS_PREFIX + userId;
        Set<String> tokens = redisTemplate.opsForSet().members(userTokensKey);
        if (tokens != null && !tokens.isEmpty()) {
            redisTemplate.delete(tokens.stream()
                    .filter(token -> token != null && !token.isBlank())
                    .map(token -> TOKEN_PREFIX + token)
                    .toList());
        }
        redisTemplate.delete(userTokensKey);
    }

    private void addToUserTokenIndex(Long userId, String token, Duration tokenTtl) {
        String userTokensKey = USER_TOKENS_PREFIX + userId;
        redisTemplate.opsForSet().add(userTokensKey, token);

        Long indexTtlSeconds = redisTemplate.getExpire(userTokensKey, TimeUnit.SECONDS);
        if (indexTtlSeconds == null || indexTtlSeconds < tokenTtl.toSeconds()) {
            redisTemplate.expire(userTokensKey, tokenTtl);
        }
    }

    private Long parseUserId(String storedUserId) {
        if (storedUserId == null) {
            return null;
        }

        try {
            Long userId = Long.valueOf(storedUserId);
            return userId > 0 ? userId : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Duration durationOf(Long seconds) {
        return seconds != null && seconds > 0
                ? Duration.ofSeconds(seconds)
                : null;
    }
}
