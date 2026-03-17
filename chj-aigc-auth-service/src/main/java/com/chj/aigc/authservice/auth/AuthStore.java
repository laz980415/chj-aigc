package com.chj.aigc.authservice.auth;

import java.util.List;
import java.util.Optional;

/**
 * 认证服务账号与会话存储接口。
 */
public interface AuthStore {
    Optional<AuthUser> findUserByUsername(String username);

    Optional<AuthUser> findUserById(String id);

    List<AuthUser> listUsers();

    List<AuthUser> listUsersByTenantId(String tenantId);

    void saveUser(AuthUser user);

    Optional<AuthSession> findSessionByToken(String token);

    void saveSession(AuthSession session);
}
