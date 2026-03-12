package com.chj.aigc.authservice.web;

import com.chj.aigc.authservice.auth.AuthInterceptor;
import com.chj.aigc.authservice.auth.AuthService;
import com.chj.aigc.authservice.auth.AuthSession;
import com.chj.aigc.authservice.web.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证服务登录与当前会话接口。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequest request) {
        AuthSession session = authService.login(request.username(), request.password());
        return ApiResponse.success(toPayload(session));
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(HttpServletRequest request) {
        AuthSession session = (AuthSession) request.getAttribute(AuthInterceptor.REQUEST_SESSION_KEY);
        return ApiResponse.success(toPayload(session));
    }

    /**
     * 提供给其他微服务使用的会话校验接口。
     */
    @GetMapping("/introspect")
    public ApiResponse<Map<String, Object>> introspect(HttpServletRequest request) {
        AuthSession session = (AuthSession) request.getAttribute(AuthInterceptor.REQUEST_SESSION_KEY);
        return ApiResponse.success(toPayload(session));
    }

    private Map<String, Object> toPayload(AuthSession session) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("token", session.token());
        payload.put("userId", session.userId());
        payload.put("username", session.username());
        payload.put("displayName", session.displayName());
        payload.put("roleKey", session.roleKey());
        payload.put("tenantId", session.tenantId());
        payload.put("expiresAt", session.expiresAt().toString());
        return payload;
    }
}
