package com.qingmou.cxnp.controller;

import com.qingmou.cxnp.entity.User;
import com.qingmou.cxnp.service.UserService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    // 声明一个成员变量,保存UserService引用
    private final UserService userService;
    //是构造方法。构造方法的名称必须与类名一致。
    public UserController(UserService userService) {
        this.userService = userService;
    }

    // List<User>是 Java 的泛型类型声明
    @GetMapping
    public List<User> list() {
        return userService.getAllUsers();
    }
}