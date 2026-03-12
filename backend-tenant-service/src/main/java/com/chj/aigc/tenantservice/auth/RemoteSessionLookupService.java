package com.chj.aigc.tenantservice.auth;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

/**
 * 通过认证服务查询会话，减少租户服务直接依赖认证表。
 */
public class RemoteSessionLookupService implements SessionLookupService {
    private final RestClient restClient;

    public RemoteSessionLookupService(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Optional<AuthSession> findSession(String token) {
        try {
            @SuppressWarnings("unchecked")
            ApiResponse<Map<String, Object>> response = restClient.get()
                    .uri("/api/auth/introspect")
                    .header(HttpHeaders.ACCEPT, "application/json")
                    .header(AuthInterceptor.TOKEN_HEADER, token)
                    .retrieve()
                    .body(ApiResponse.class);
            if (response == null || response.code() != 0 || response.data() == null) {
                return Optional.empty();
            }
            return Optional.of(toSession(response.data()));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private AuthSession toSession(Map<String, Object> data) {
        return new AuthSession(
                String.valueOf(data.get("token")),
                value(data, "userId"),
                value(data, "username"),
                value(data, "displayName"),
                value(data, "roleKey"),
                value(data, "tenantId"),
                Instant.now(),
                Instant.parse(String.valueOf(data.get("expiresAt")))
        );
    }

    private String value(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value == null ? null : String.valueOf(value);
    }
}
