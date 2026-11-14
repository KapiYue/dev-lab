package com.qingmou.cxnp.controller;

import com.qingmou.cxnp.annotation.RequirePermission;
import com.qingmou.cxnp.common.Result;
import com.qingmou.cxnp.constant.PermissionConstants;
import com.qingmou.cxnp.entity.User;
import com.qingmou.cxnp.service.AuthService;
import com.qingmou.cxnp.service.UserService;

import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {
    // 声明一个成员变量,保存UserService引用
    private final UserService userService;
    private final AuthService authService;
    //是构造方法。构造方法的名称必须与类名一致。
    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    // List<User>是 Java 的泛型类型声明
    @GetMapping
    public List<User> list() {
        return userService.getAllUsers();
    }

    @PostMapping("/{userId}/sessions/revoke")
    @RequirePermission(PermissionConstants.RBAC_USER_SESSION_REVOKE)
    public Result<Void> revokeAllSessions(
            @PathVariable @Positive(message = "用户 ID 必须大于 0") Long userId
    ) {
        authService.revokeAllUserSessions(userId);
        return Result.success(null);
    }
}
