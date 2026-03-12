package com.chj.aigc.tenantservice.auth;

import java.util.List;
import java.util.Optional;

/**
 * 认证存储接口。
 */
public interface AuthStore {
    List<AuthUser> listUsers();

    Optional<AuthUser> findUserById(String userId);

    Optional<AuthUser> findUserByUsername(String username);

    void saveUser(AuthUser user);

    Optional<AuthSession> findSessionByToken(String token);

    void saveSession(AuthSession session);
}
