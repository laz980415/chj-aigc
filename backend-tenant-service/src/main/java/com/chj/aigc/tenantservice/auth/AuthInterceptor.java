package com.chj.aigc.tenantservice.auth;

import com.chj.aigc.tenantservice.web.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 租户服务统一登录态校验。
 */
public final class AuthInterceptor implements HandlerInterceptor {
    public static final String TOKEN_HEADER = "X-Auth-Token";
    public static final String REQUEST_SESSION_KEY = "currentSession";

    private final SessionLookupService sessionLookupService;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(SessionLookupService sessionLookupService, ObjectMapper objectMapper) {
        this.sessionLookupService = sessionLookupService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/api/health")) {
            return true;
        }

        String token = request.getHeader(TOKEN_HEADER);
        if (token == null || token.isBlank()) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, "未登录或令牌缺失");
            return false;
        }

        AuthSession session = sessionLookupService.findSession(token).orElse(null);
        if (session == null) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, "登录已失效，请重新登录");
            return false;
        }

        if (path.startsWith("/api/tenant/")
                && !Set.of("tenant_owner", "tenant_member").contains(session.roleKey())) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, "当前账号没有租户权限");
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
