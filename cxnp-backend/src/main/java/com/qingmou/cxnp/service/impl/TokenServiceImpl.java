package com.qingmou.cxnp.service.impl;

import com.qingmou.cxnp.service.TokenService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

@Service
public class TokenServiceImpl implements TokenService {

    private static final String TOKEN_PREFIX = "login:token:";
    private static final Duration TOKEN_TTL = Duration.ofHours(2);
    private final StringRedisTemplate redisTemplate;

    // 初始化随机数生成器
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenServiceImpl(StringRedisTemplate redisTemplate){
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String createToken(Long userId){
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
    public Long verifyToken(String token){
        if(token == null || token.isBlank()){
            return null;
        }
        String userId = redisTemplate.opsForValue().get(
                TOKEN_PREFIX + token
        );
        if (userId == null) {
            return null;
        }

        try {
            return Long.valueOf(userId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
