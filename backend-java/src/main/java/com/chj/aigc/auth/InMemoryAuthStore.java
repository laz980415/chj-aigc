package com.chj.aigc.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class InMemoryAuthStore implements AuthStore {
    private final List<AuthUser> users = new ArrayList<>();
    private final List<AuthSession> sessions = new ArrayList<>();

    @Override
    public List<AuthUser> listUsers() {
        return List.copyOf(users);
    }

    @Override
    public Optional<AuthUser> findUserById(String userId) {
        return users.stream()
                .filter(user -> user.id().equals(userId))
                .findFirst();
    }

    @Override
    public Optional<AuthUser> findUserByUsername(String username) {
        return users.stream()
                .filter(user -> user.username().equalsIgnoreCase(username))
                .findFirst();
    }

    @Override
    public void saveUser(AuthUser user) {
        users.removeIf(existing -> existing.id().equals(user.id()));
        users.add(Objects.requireNonNull(user, "user"));
    }

    @Override
    public Optional<AuthSession> findSessionByToken(String token) {
        return sessions.stream()
                .filter(session -> session.token().equals(token))
                .findFirst();
    }

    @Override
    public void saveSession(AuthSession session) {
        sessions.removeIf(existing -> existing.token().equals(session.token()));
        sessions.add(Objects.requireNonNull(session, "session"));
    }
}
