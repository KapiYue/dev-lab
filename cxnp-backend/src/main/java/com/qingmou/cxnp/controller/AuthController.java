package com.qingmou.cxnp.controller;

import com.qingmou.cxnp.dto.LoginRequest;
import com.qingmou.cxnp.service.AuthService;
import com.qingmou.cxnp.common.Result;
import com.qingmou.cxnp.context.UserContext;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<String> login(
            @Valid @RequestBody LoginRequest request) {
        String token = authService.login(request);
        return Result.success(token);
    }

    @PostMapping("/logout")
    public Result<Void> logout(
            @RequestHeader("Authorization") String authorization) {
        String token = authorization.substring(7).trim();

        authService.logout(UserContext.getUserId(), token);

        return Result.success(null);
    }
}