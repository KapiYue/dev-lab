package com.qingmou.cxnp.controller;

import com.qingmou.cxnp.dto.LoginRequest;
import com.qingmou.cxnp.service.AuthService;
import com.qingmou.cxnp.common.Result;
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
            @Valid @RequestBody LoginRequest request
    ) {
        String token = authService.login(request);
        return Result.success(token);
    }
}