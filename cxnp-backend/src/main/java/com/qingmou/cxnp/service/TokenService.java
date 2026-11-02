package com.qingmou.cxnp.service;

public interface TokenService {
    String createToken(Long userId);

    Long verifyToken(String token);
}
