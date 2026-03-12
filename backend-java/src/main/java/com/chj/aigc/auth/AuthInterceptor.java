package com.chj.aigc.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 统一校验 API 请求上的登录态和角色边界。
 * 平台接口只允许超管访问，租户接口允许租户角色访问，再由控制器继续收窄写权限。
 */
public final class AuthInterceptor implements HandlerInterceptor {
    public static final String TOKEN_HEADER = "X-Auth-Token";
    public static final String REQUEST_SESSION_KEY = "currentSession";

    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/api/health")
                || path.startsWith("/api/db-info")
                || path.equals("/api/auth/login")) {
            return true;
        }

        String token = request.getHeader(TOKEN_HEADER);
        if (token == null || token.isBlank()) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, "{\"message\":\"未登录或令牌缺失\"}");
            return false;
        }

        AuthSession session = authService.findSession(token).orElse(null);
        if (session == null) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, "{\"message\":\"登录已失效，请重新登录\"}");
            return false;
        }

        if (path.startsWith("/api/admin/") && !session.roleKey().equals("platform_super_admin")) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, "{\"message\":\"当前账号没有超管权限\"}");
            return false;
        }

        if (path.startsWith("/api/tenant/") && !Set.of("platform_super_admin", "tenant_owner", "tenant_member").contains(session.roleKey())) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, "{\"message\":\"当前账号没有租户权限\"}");
            return false;
        }

        request.setAttribute(REQUEST_SESSION_KEY, session);
        return true;
    }

    private void writeJson(HttpServletResponse response, int statusCode, String body) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(body);
    }
}
