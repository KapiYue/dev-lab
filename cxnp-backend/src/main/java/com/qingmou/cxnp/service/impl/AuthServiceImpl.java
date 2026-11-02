package com.qingmou.cxnp.service.impl;

import com.qingmou.cxnp.dto.LoginRequest;
import com.qingmou.cxnp.entity.SysUser;
import com.qingmou.cxnp.mapper.SysUserMapper;
import com.qingmou.cxnp.service.AuthService;
import com.qingmou.cxnp.service.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.qingmou.cxnp.exception.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {
    // 声明成员变量，但是通过 @Autowired 注入就不需要构造方法注入
    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    // 构造方法注入，企业写法，推荐的写法
    public AuthServiceImpl(
            SysUserMapper sysUserMapper,
            PasswordEncoder passwordEncoder,
            TokenService tokenService
    ) {
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }



    @Override
    public String login(LoginRequest request) {
// 1.根据用户名查询
        SysUser user = sysUserMapper.selectByUsername(
                request.getUsername()
        );
// 2.判断用户是否存在 是否异常
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
        // throw new IllegalArgumentException("用户名或密码错误"); 改成自己写的Authentication
            throw new AuthenticationException("用户名或密码错误");
        }

        // 3：增加 BCrypt 密码校验、Token 生成和 Redis 会话保存。
        if(user.getPassword() == null
        || !passwordEncoder.matches(request.getPassword(), user.getPassword())
        ){
            throw new AuthenticationException("用户名或者密码错误");
        }
        // 4.生成 Token 并写入Redis
        return tokenService.createToken(user.getId());
        // 在认证实现完成前，不允许返回成功登录结果。
    }
}