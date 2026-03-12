package com.chj.aigc.tenantservice.auth;

import java.util.Optional;

/**
 * 租户服务会话查询抽象，便于从本地存储逐步切换到认证服务。
 */
public interface SessionLookupService {
    /**
     * 根据 token 查询有效会话。
     */
    Optional<AuthSession> findSession(String token);
}
