/*
UserContext负责保存 读取 清理当前用户ID。
LoginInterceptor负责读取token 验证并保存用户ID
 */
package com.qingmou.cxnp.config;

import com.qingmou.cxnp.context.UserContext;
import com.qingmou.cxnp.interceptor.LoginInterceptor;
import com.qingmou.cxnp.interceptor.PermissionInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// WebMvcConfigurer属于Spring MVC 提供的配置扩展接口
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;
    private final PermissionInterceptor permissionInterceptor;

    public WebMvcConfig(
            LoginInterceptor loginInterceptor,
            PermissionInterceptor permissionInterceptor
    ){
        this.loginInterceptor = loginInterceptor;
        this.permissionInterceptor = permissionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry){
    // 将自己写的拦截器添加到mvc拦截链中
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login")
                .order(1);

        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login")
                .order(2);
    }
}

