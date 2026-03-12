package com.chj.aigc.tenantservice.web;

import com.chj.aigc.tenantservice.auth.AuthInterceptor;
import com.chj.aigc.tenantservice.auth.AuthService;
import com.chj.aigc.tenantservice.auth.AuthSession;
import com.chj.aigc.tenantservice.web.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 租户服务登录接口。
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
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("token", session.token());
        payload.put("userId", session.userId());
        payload.put("username", session.username());
        payload.put("displayName", session.displayName());
        payload.put("roleKey", session.roleKey());
        payload.put("tenantId", session.tenantId());
        payload.put("expiresAt", session.expiresAt().toString());
        return ApiResponse.success(payload);
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(HttpServletRequest request) {
        AuthSession session = (AuthSession) request.getAttribute(AuthInterceptor.REQUEST_SESSION_KEY);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", session.userId());
        payload.put("username", session.username());
        payload.put("displayName", session.displayName());
        payload.put("roleKey", session.roleKey());
        payload.put("tenantId", session.tenantId());
        payload.put("expiresAt", session.expiresAt().toString());
        return ApiResponse.success(payload);
    }
}
