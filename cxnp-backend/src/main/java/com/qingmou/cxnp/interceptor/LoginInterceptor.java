
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

    public LoginInterceptor(TokenService tokenService,
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
        // 防止复用线程时残留上一次请求的用户信息
        UserContext.clear();

        // 1. 获取 Authorization 请求头
        String authorization = request.getHeader("Authorization");

        Long userId = null;

        // 2. 提取验证。如果没有 Bearer Token，暂时放行
        if (authorization != null &&
                authorization.startsWith("Bearer ")) {
            // 3. 提取 Token,请求头是Authorization: Bearer abc123
            String token = authorization.substring(7).trim();

            if(!token.isEmpty()){
                // Redis验证
                userId = tokenService.verifyToken(token);
            }
        }

    // 4. 验证成功
        if(userId != null){
            UserContext.setUserId(userId);
        }
    // 5. 判断当前接口是否需要登录
        String path  = request.getServletPath();

        boolean requiresLogin = path.equals("/api/party-members")
                || path.startsWith("/api/party-members/");

        // 6.需要登录 但当前用户不存在.
//        if(requiresLogin && userId == null){
//            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//            return false;
//        }
//        6.未登录时统一返回JSON(未登录 + token过期两个)
        if(requiresLogin && userId == null){
            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name()
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
        // 7. 允许继续运行Controller
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        // 请求处理结束后清理 ThreadLocal
        UserContext.clear();
    }
}
