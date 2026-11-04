
package com.qingmou.cxnp.interceptor;

import com.qingmou.cxnp.annotation.RequirePermission;
import com.qingmou.cxnp.context.UserContext;
import com.qingmou.cxnp.service.PermissionService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class PermissionInterceptor implements HandlerInterceptor {

    private final PermissionService permissionService;

    // 构造器注入 PermissionService
    public PermissionInterceptor(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {

        // 1. 非 Controller 方法，放行；handler就代表 Spring MVC 已经匹配好，其实在拦截器之前就知道了
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 2. 获取 Controller 方法上的权限注解
        RequirePermission requirePermission =
                handlerMethod.getMethodAnnotation(
                        RequirePermission.class
                );

        // 3. 没有权限注解，跳过功能权限检查
        if (requirePermission == null) {
            return true;
        }

        // 4. 获取所需的权限编码
        String permissionCode = requirePermission.value();

        System.out.println(
                "[PermissionInterceptor] 开始校验权限：" + permissionCode
        );

        System.out.println(
                "[PermissionInterceptor] 当前用户ID：" + UserContext.getUserId()
        );

        // 5. 调用 PermissionService 进行权限校验
        // 无权限时会抛出 SecurityException
        permissionService.checkCurrentUserPermission(
                permissionCode
        );

        // 6. 只有校验成功才会执行到这里
        System.out.println(
                "[PermissionInterceptor] 权限校验通过：" + permissionCode
        );

        return true;
    }
}
