package com.chj.aigc.tenantservice.auth;

import java.time.Instant;
import java.util.Optional;

/**
 * 直接从本地库查询会话，适合测试和过渡阶段。
 */
public class LocalSessionLookupService implements SessionLookupService {
    private final AuthStore authStore;

    public LocalSessionLookupService(AuthStore authStore) {
        this.authStore = authStore;
    }

    @Override
    public Optional<AuthSession> findSession(String token) {
        return authStore.findSessionByToken(token)
                .filter(session -> session.expiresAt().isAfter(Instant.now()));
    }
}
