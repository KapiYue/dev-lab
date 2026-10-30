package com.qingmou.cxnp.service;

import com.qingmou.cxnp.entity.User;
import com.qingmou.cxnp.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    // 没有使用new useMapper原因，UserMapper 通常是一个接口，MyBatis把它注册到Spring容器中。
    // UserService也是Spring容器创建，就从容器中找到 MyBatis 注册的代理对象
    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public List<User> getAllUsers() {
        return userMapper.selectAll();
    }
}