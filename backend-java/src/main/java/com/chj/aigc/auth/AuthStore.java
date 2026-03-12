package com.chj.aigc.auth;

import java.util.List;
import java.util.Optional;

public interface AuthStore {
    List<AuthUser> listUsers();

    Optional<AuthUser> findUserById(String userId);

    Optional<AuthUser> findUserByUsername(String username);

    void saveUser(AuthUser user);

    Optional<AuthSession> findSessionByToken(String token);

    void saveSession(AuthSession session);
}
