package com.qingmou.cxnp.service;

import com.qingmou.cxnp.dto.LoginRequest;

public interface AuthService {

    // 返回的是登录凭证，字符串
    String login(LoginRequest request);

    void logout(Long authenticatedUserId, String token);

    void revokeAllUserSessions(Long targetUserId);
}
