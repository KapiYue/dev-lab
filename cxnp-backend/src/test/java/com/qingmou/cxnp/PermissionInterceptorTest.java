
package com.qingmou.cxnp;

import com.qingmou.cxnp.annotation.RequirePermission;
import com.qingmou.cxnp.interceptor.PermissionInterceptor;
import com.qingmou.cxnp.service.PermissionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PermissionInterceptorTest {

    private PermissionService permissionService;
    private PermissionInterceptor interceptor;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    // 模拟 Controller，专门用于测试权限注解
    public static class TestController {

        @RequirePermission("party:member:list")
        public void list() {
        }

        @RequirePermission("party:member:detail")
        public void detail() {
        }

        public void publicMethod() {
        }
    }

    @BeforeEach
    void setUp() {

        // 创建模拟的权限 Service
        permissionService = mock(PermissionService.class);

        // 注入模拟 Service
        interceptor = new PermissionInterceptor(
                permissionService
        );

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    // 1. 有权限时允许通过
    @Test
    void testWithPermission() throws Exception {

        HandlerMethod handler = new HandlerMethod(
                new TestController(),
                TestController.class.getMethod("list")
        );

        boolean result = interceptor.preHandle(
                request,
                response,
                handler
        );

        assertTrue(result);

        // 确认拦截器调用了权限校验
        verify(permissionService).checkCurrentUserPermission(
                "party:member:list"
        );
    }

    // 2. 无权限时抛出异常
    @Test
    void testWithoutPermission() throws Exception {

        HandlerMethod handler = new HandlerMethod(
                new TestController(),
                TestController.class.getMethod("list")
        );

        // 模拟权限校验失败
        doThrow(new SecurityException(
                "无功能权限：party:member:list"
        ))
                .when(permissionService)
                .checkCurrentUserPermission("party:member:list");

        SecurityException exception = assertThrows(
                SecurityException.class,
                () -> interceptor.preHandle(
                        request,
                        response,
                        handler
                )
        );

        assertEquals(
                "无功能权限：party:member:list",
                exception.getMessage()
        );

        verify(permissionService).checkCurrentUserPermission(
                "party:member:list"
        );
    }

    // 3. 没有权限注解时跳过功能权限检查
    @Test
    void testWithoutAnnotation() throws Exception {

        HandlerMethod handler = new HandlerMethod(
                new TestController(),
                TestController.class.getMethod("publicMethod")
        );

        boolean result = interceptor.preHandle(
                request,
                response,
                handler
        );

        assertTrue(result);

        // 不应该调用权限 Service
        verifyNoInteractions(permissionService);
    }

    // 4. 非 HandlerMethod 时跳过注解检查
    @Test
    void testNonHandlerMethod() throws Exception {

        boolean result = interceptor.preHandle(
                request,
                response,
                new Object()
        );

        assertTrue(result);

        verifyNoInteractions(permissionService);
    }
}
