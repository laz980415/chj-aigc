package com.chj.aigc.authservice.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chj.aigc.authservice.persistence.mapper.AuthMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MybatisAuthStoreTest {

    @Test
    void mapsLowercaseColumnNamesFromAuthQueries() {
        AuthMapper authMapper = mock(AuthMapper.class);
        MybatisAuthStore store = new MybatisAuthStore(authMapper);
        Instant createdAt = Instant.parse("2026-03-18T10:00:00Z");
        Instant expiresAt = Instant.parse("2026-03-18T22:00:00Z");

        when(authMapper.findUserByUsername("tenant_owner")).thenReturn(Map.of(
                "id", "user-1",
                "username", "tenant_owner",
                "password", "encoded-password",
                "displayname", "Tenant Owner",
                "rolekey", "tenant_owner",
                "tenantid", "tenant-demo",
                "active", true
        ));
        when(authMapper.findSessionByToken("token-1")).thenReturn(Map.of(
                "token", "token-1",
                "userid", "user-1",
                "username", "tenant_owner",
                "displayname", "Tenant Owner",
                "rolekey", "tenant_owner",
                "tenantid", "tenant-demo",
                "createdat", Timestamp.from(createdAt),
                "expiresat", Timestamp.from(expiresAt)
        ));

        AuthUser user = store.findUserByUsername("tenant_owner").orElseThrow();
        AuthSession session = store.findSessionByToken("token-1").orElseThrow();

        assertEquals("Tenant Owner", user.displayName());
        assertEquals("tenant_owner", user.roleKey());
        assertEquals("tenant-demo", user.tenantId());
        assertTrue(user.active());

        assertEquals("user-1", session.userId());
        assertEquals("Tenant Owner", session.displayName());
        assertEquals("tenant_owner", session.roleKey());
        assertEquals("tenant-demo", session.tenantId());
        assertEquals(createdAt, session.createdAt());
        assertEquals(expiresAt, session.expiresAt());
    }

    @Test
    void mapsUserListsUsingLowercaseTenantIdColumn() {
        AuthMapper authMapper = mock(AuthMapper.class);
        MybatisAuthStore store = new MybatisAuthStore(authMapper);

        when(authMapper.listUsersByTenantId("tenant-demo")).thenReturn(List.of(Map.of(
                "id", "user-2",
                "username", "member",
                "password", "encoded",
                "displayname", "Member",
                "rolekey", "tenant_member",
                "tenantid", "tenant-demo",
                "active", true
        )));

        List<AuthUser> users = store.listUsersByTenantId("tenant-demo");

        assertEquals(1, users.size());
        assertEquals("tenant-demo", users.getFirst().tenantId());
        assertEquals("Member", users.getFirst().displayName());
    }
}
