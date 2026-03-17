package com.chj.aigc.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 通过 HTTP 调用认证服务的账号能力实现。
 * 平台服务不再直接查询 auth_users / auth_sessions 表，
 * 所有账号和会话操作都委托给 chj-aigc-auth-service。
 */
public class RemoteAuthService implements PlatformAuthService {
    private final String authServiceUri;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public RemoteAuthService(String authServiceUri) {
        this.authServiceUri = authServiceUri.replaceAll("/$", "");
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Optional<AuthSession> findSession(String token) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(authServiceUri + "/api/auth/introspect"))
                    .header("X-Auth-Token", token)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return Optional.empty();
            Map<String, Object> envelope = objectMapper.readValue(response.body(), new TypeReference<>() {});
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) envelope.get("data");
            if (data == null) return Optional.empty();
            return Optional.of(mapSession(token, data));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public List<AuthUser> listUsers() {
        return getList("/api/auth/users");
    }

    @Override
    public List<AuthUser> listTenantUsers(String tenantId) {
        return getList("/api/auth/users/tenant/" + tenantId);
    }

    @Override
    public List<String> builtinRoles() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(authServiceUri + "/api/auth/roles"))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> envelope = objectMapper.readValue(response.body(), new TypeReference<>() {});
            @SuppressWarnings("unchecked")
            List<String> data = (List<String>) envelope.get("data");
            return data != null ? data : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public AuthUser createUser(String userId, String username, String password,
                               String displayName, String roleKey, String tenantId) {
        try {
            Map<String, Object> body = Map.of(
                    "userId", userId, "username", username, "password", password,
                    "displayName", displayName, "roleKey", roleKey,
                    "tenantId", tenantId != null ? tenantId : ""
            );
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(authServiceUri + "/api/auth/users"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> envelope = objectMapper.readValue(response.body(), new TypeReference<>() {});
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) envelope.get("data");
            return mapUser(data);
        } catch (Exception e) {
            throw new RuntimeException("创建账号失败：" + e.getMessage(), e);
        }
    }

    private List<AuthUser> getList(String path) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(authServiceUri + path))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> envelope = objectMapper.readValue(response.body(), new TypeReference<>() {});
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) envelope.get("data");
            if (data == null) return List.of();
            return data.stream().map(this::mapUser).toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private AuthUser mapUser(Map<String, Object> row) {
        return new AuthUser(
                str(row, "id"), str(row, "username"), "",
                str(row, "displayName"), str(row, "roleKey"),
                str(row, "tenantId"), Boolean.TRUE.equals(row.get("active"))
        );
    }

    private AuthSession mapSession(String token, Map<String, Object> data) {
        String expiresAtStr = str(data, "expiresAt");
        Instant expiresAt = expiresAtStr != null ? Instant.parse(expiresAtStr) : Instant.now().plusSeconds(43200);
        return new AuthSession(
                token,
                str(data, "userId"), str(data, "username"), str(data, "displayName"),
                str(data, "roleKey"), str(data, "tenantId"),
                Instant.now(), expiresAt
        );
    }

    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }
}
