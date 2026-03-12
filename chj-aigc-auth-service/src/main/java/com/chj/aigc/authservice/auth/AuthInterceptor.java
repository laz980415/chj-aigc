package com.chj.aigc.authservice.auth;

import com.chj.aigc.authservice.web.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 认证服务登录态校验。
 */
public class AuthInterceptor implements HandlerInterceptor {
    public static final String TOKEN_HEADER = "X-Auth-Token";
    public static final String REQUEST_SESSION_KEY = "currentSession";

    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(AuthService authService, ObjectMapper objectMapper) {
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/api/health") || path.equals("/api/auth/login")) {
            return true;
        }

        String token = request.getHeader(TOKEN_HEADER);
        if (token == null || token.isBlank()) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, "未登录或令牌缺失");
            return false;
        }

        AuthSession session = authService.findSession(token).orElse(null);
        if (session == null) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, "登录已失效，请重新登录");
            return false;
        }

        request.setAttribute(REQUEST_SESSION_KEY, session);
        return true;
    }

    private void writeJson(HttpServletResponse response, int statusCode, String message) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.failure(statusCode, message)));
    }
}
