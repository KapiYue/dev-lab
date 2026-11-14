package com.qingmou.cxnp.service.impl;

import com.qingmou.cxnp.dto.LoginRequest;
import com.qingmou.cxnp.entity.SysUser;
import com.qingmou.cxnp.mapper.SysUserMapper;
import com.qingmou.cxnp.service.AuthService;
import com.qingmou.cxnp.service.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.qingmou.cxnp.exception.AuthenticationException;
import com.qingmou.cxnp.exception.LoginTemporarilyLockedException;
import com.qingmou.cxnp.exception.ResourceNotFoundException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;

@Service
public class AuthServiceImpl implements AuthService {
    private static final String LOGIN_FAILURE_PREFIX = "login:failure:";
    private static final String LOGIN_LOCK_PREFIX = "login:lock:";
    private static final Duration LOGIN_FAILURE_WINDOW = Duration.ofMinutes(15);
    private static final int MAX_LOGIN_FAILURES = 5;

    // 声明成员变量，但是通过 @Autowired 注入就不需要构造方法注入
    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final StringRedisTemplate redisTemplate;

    // 构造方法注入，企业写法，推荐的写法
    public AuthServiceImpl(
            SysUserMapper sysUserMapper,
            PasswordEncoder passwordEncoder,
            TokenService tokenService,
            StringRedisTemplate redisTemplate) {
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String login(LoginRequest request) {
        String loginIdentifier = normalizeLoginIdentifier(request.getUsername());
        if (isTemporarilyLocked(loginIdentifier)) {
            throw new LoginTemporarilyLockedException();
        }

        SysUser user = sysUserMapper.selectByUsername(loginIdentifier);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            recordLoginFailure(loginIdentifier);
            throw new AuthenticationException("用户名或密码错误");
        }

        if (user.getPassword() == null
                || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            recordLoginFailure(loginIdentifier);
            throw new AuthenticationException("用户名或密码错误");
        }

        clearLoginFailures(loginIdentifier);
        return tokenService.createToken(user.getId());
    }

    @Override
    public void logout(Long authenticatedUserId, String token) {

        // 1. 校验参数
        if (authenticatedUserId == null
                || token == null
                || token.isBlank()) {
            throw new AuthenticationException("当前会话无效");
        }

        // 2. 验证 Token 对应的用户
        Long tokenUserId = tokenService.verifyToken(token);

        // 3. 校验 Token 是否属于当前认证用户
        if (tokenUserId == null
                || !authenticatedUserId.equals(tokenUserId)) {
            throw new AuthenticationException("当前会话无效");
        }

        // 4. 只撤销当前 Token
        tokenService.revokeToken(token);
    }

    @Override
    public void revokeAllUserSessions(Long targetUserId) {
        if (targetUserId == null || targetUserId <= 0) {
            throw new IllegalArgumentException("用户 ID 必须大于 0");
        }
        if (sysUserMapper.selectById(targetUserId) == null) {
            throw new ResourceNotFoundException("目标用户不存在");
        }

        tokenService.revokeAllUserTokens(targetUserId);
    }

    private String normalizeLoginIdentifier(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isTemporarilyLocked(String loginIdentifier) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(LOGIN_LOCK_PREFIX + loginIdentifier));
    }

    private void recordLoginFailure(String loginIdentifier) {
        String failureKey = LOGIN_FAILURE_PREFIX + loginIdentifier;
        Long failures = redisTemplate.opsForValue().increment(failureKey);
        if (failures != null && failures == 1) {
            redisTemplate.expire(failureKey, LOGIN_FAILURE_WINDOW);
        }
        if (failures != null && failures >= MAX_LOGIN_FAILURES) {
            redisTemplate.delete(failureKey);
            redisTemplate.opsForValue().set(
                    LOGIN_LOCK_PREFIX + loginIdentifier,
                    "1",
                    LOGIN_FAILURE_WINDOW);
        }
    }

    private void clearLoginFailures(String loginIdentifier) {
        redisTemplate.delete(java.util.List.of(
                LOGIN_FAILURE_PREFIX + loginIdentifier,
                LOGIN_LOCK_PREFIX + loginIdentifier));
    }
}
