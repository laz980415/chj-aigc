package com.chj.aigc.authservice.web;

import com.chj.aigc.authservice.auth.AuthInterceptor;
import com.chj.aigc.authservice.auth.AuthService;
import com.chj.aigc.authservice.auth.AuthSession;
import com.chj.aigc.authservice.auth.AuthUser;
import com.chj.aigc.authservice.web.dto.CreateUserRequest;
import com.chj.aigc.authservice.web.dto.LoginRequest;
import com.chj.aigc.authservice.web.dto.UpdateUserRoleRequest;
import com.chj.aigc.authservice.web.dto.UpdateUserStatusRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证服务接口：登录、会话校验、账号管理。
 * 认证服务是 auth_users / auth_sessions 的唯一拥有者，
 * 其他微服务通过这里的 HTTP 接口访问账号数据，不直接查询这两张表。
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
        return ApiResponse.success(toSessionPayload(session));
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(HttpServletRequest request) {
        return ApiResponse.success(toSessionPayload(currentSession(request)));
    }

    /** 供其他微服务校验 token 并获取会话信息。 */
    @GetMapping("/introspect")
    public ApiResponse<Map<String, Object>> introspect(HttpServletRequest request) {
        return ApiResponse.success(toSessionPayload(currentSession(request)));
    }

    /** 返回所有账号，供平台超管使用。 */
    @GetMapping("/users")
    public ApiResponse<List<Map<String, Object>>> listUsers() {
        return ApiResponse.success(authService.listUsers().stream().map(this::toUserPayload).toList());
    }

    /** 返回指定租户下的成员账号。 */
    @GetMapping("/users/tenant/{tenantId}")
    public ApiResponse<List<Map<String, Object>>> listTenantUsers(@PathVariable String tenantId) {
        return ApiResponse.success(authService.listTenantUsers(tenantId).stream().map(this::toUserPayload).toList());
    }

    /** 返回内置角色列表。 */
    @GetMapping("/roles")
    public ApiResponse<List<String>> builtinRoles() {
        return ApiResponse.success(authService.builtinRoles());
    }

    /** 创建账号，平台超管可创建任意角色。 */
    @PostMapping("/users")
    public ApiResponse<Map<String, Object>> createUser(@RequestBody CreateUserRequest request) {
        AuthUser user = authService.createUser(
                request.userId(), request.username(), request.password(),
                request.displayName(), request.roleKey(),
                request.tenantId() == null || request.tenantId().isBlank() ? null : request.tenantId()
        );
        return ApiResponse.success(toUserPayload(user));
    }

    /** 创建租户成员，角色限制为 tenant_owner 或 tenant_member。 */
    @PostMapping("/users/tenant")
    public ApiResponse<Map<String, Object>> createTenantUser(@RequestBody CreateUserRequest request) {
        AuthUser user = authService.createTenantUser(
                request.userId(), request.username(), request.password(),
                request.displayName(), request.roleKey(), request.tenantId()
        );
        return ApiResponse.success(toUserPayload(user));
    }

    /** 更新租户成员启用状态。 */
    @PostMapping("/users/{userId}/status")
    public ApiResponse<Map<String, Object>> updateUserStatus(
            @PathVariable String userId,
            @RequestBody UpdateUserStatusRequest request,
            HttpServletRequest httpRequest
    ) {
        AuthSession session = currentSession(httpRequest);
        if (session.userId().equals(userId) && !request.active()) {
            throw new IllegalArgumentException("不能停用当前登录账号");
        }
        return ApiResponse.success(toUserPayload(authService.updateUserStatus(userId, session.tenantId(), request.active())));
    }

    /** 调整租户成员角色。 */
    @PostMapping("/users/{userId}/role")
    public ApiResponse<Map<String, Object>> updateUserRole(
            @PathVariable String userId,
            @RequestBody UpdateUserRoleRequest request,
            HttpServletRequest httpRequest
    ) {
        AuthSession session = currentSession(httpRequest);
        if (session.userId().equals(userId)) {
            throw new IllegalArgumentException("不能修改当前登录账号的角色");
        }
        return ApiResponse.success(toUserPayload(authService.updateUserRole(userId, session.tenantId(), request.roleKey())));
    }

    private AuthSession currentSession(HttpServletRequest request) {
        return (AuthSession) request.getAttribute(AuthInterceptor.REQUEST_SESSION_KEY);
    }

    private Map<String, Object> toSessionPayload(AuthSession session) {
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

    private Map<String, Object> toUserPayload(AuthUser user) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", user.id());
        payload.put("username", user.username());
        payload.put("displayName", user.displayName());
        payload.put("roleKey", user.roleKey());
        payload.put("tenantId", user.tenantId());
        payload.put("active", user.active());
        return payload;
    }
}
