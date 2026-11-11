package com.qingmou.cxnp.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingmou.cxnp.common.Result;
import com.qingmou.cxnp.context.UserContext;
import com.qingmou.cxnp.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    private final TokenService tokenService;
    private final ObjectMapper objectMapper;

    public LoginInterceptor(
            TokenService tokenService,
            ObjectMapper objectMapper
    ) {
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws IOException {

        // 1. 清理线程可能残留的用户信息
        UserContext.clear();

        // 2. 获取 Authorization 请求头
        String authorization = request.getHeader("Authorization");

        Long userId = null;

        // 3. 提取并验证 Bearer Token
        if (authorization != null
                && authorization.startsWith("Bearer ")) {

            String token = authorization.substring(7).trim();

            if (!token.isEmpty()) {
                // 通过 TokenService 验证 Redis Token
                userId = tokenService.verifyToken(token);
            }
        }

        // 4. 所有进入当前拦截器的 API 都要求登录
        // /api/auth/login 已在 WebMvcConfig 中排除
        if (userId == null) {

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );

            response.setContentType("application/json");

            response.setCharacterEncoding(
                    StandardCharsets.UTF_8.name()
            );

            Result<?> result = Result.error(
                    401,
                    "未登录或登录已过期"
            );

            response.getWriter().write(
                    objectMapper.writeValueAsString(result)
            );

            return false;
        }

        // 5. Token 验证通过，设置当前用户 ID
        UserContext.setUserId(userId);

        // 6. 允许进入权限拦截器和 Controller
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        // 请求处理结束，清理 ThreadLocal
        UserContext.clear();
    }
}