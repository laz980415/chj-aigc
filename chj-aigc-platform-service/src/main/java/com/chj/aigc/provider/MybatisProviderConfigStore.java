package com.chj.aigc.provider;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class MybatisProviderConfigStore implements ProviderConfigStore {

    private final JdbcClient jdbc;

    public MybatisProviderConfigStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ProviderConfig> listAll() {
        return jdbc.sql("SELECT * FROM provider_configs ORDER BY provider_id")
                .query((rs, i) -> map(Map.of(
                        "id", rs.getString("id"),
                        "provider_id", rs.getString("provider_id"),
                        "display_name", rs.getString("display_name"),
                        "api_base_url", rs.getString("api_base_url"),
                        "api_key_encrypted", rs.getString("api_key_encrypted"),
                        "enabled", rs.getBoolean("enabled"),
                        "updated_by", rs.getString("updated_by"),
                        "updated_at", rs.getTimestamp("updated_at")
                )))
                .list();
    }

    @Override
    public Optional<ProviderConfig> findByProviderId(String providerId) {
        return jdbc.sql("SELECT * FROM provider_configs WHERE provider_id = :providerId")
                .param("providerId", providerId)
                .query((rs, i) -> map(Map.of(
                        "id", rs.getString("id"),
                        "provider_id", rs.getString("provider_id"),
                        "display_name", rs.getString("display_name"),
                        "api_base_url", rs.getString("api_base_url"),
                        "api_key_encrypted", rs.getString("api_key_encrypted"),
                        "enabled", rs.getBoolean("enabled"),
                        "updated_by", rs.getString("updated_by"),
                        "updated_at", rs.getTimestamp("updated_at")
                )))
                .optional();
    }

    @Override
    public void save(ProviderConfig config) {
        jdbc.sql("""
                INSERT INTO provider_configs
                  (id, provider_id, display_name, api_base_url, api_key_encrypted, enabled, updated_by, updated_at)
                VALUES
                  (:id, :providerId, :displayName, :apiBaseUrl, :apiKey, :enabled, :updatedBy, :updatedAt)
                ON CONFLICT (provider_id) DO UPDATE SET
                  display_name = EXCLUDED.display_name,
                  api_base_url = EXCLUDED.api_base_url,
                  api_key_encrypted = EXCLUDED.api_key_encrypted,
                  enabled = EXCLUDED.enabled,
                  updated_by = EXCLUDED.updated_by,
                  updated_at = EXCLUDED.updated_at
                """)
                .param("id", config.id())
                .param("providerId", config.providerId())
                .param("displayName", config.displayName())
                .param("apiBaseUrl", config.apiBaseUrl())
                .param("apiKey", config.apiKey())
                .param("enabled", config.enabled())
                .param("updatedBy", config.updatedBy())
                .param("updatedAt", Timestamp.from(config.updatedAt()))
                .update();
    }

    @Override
    public void setEnabled(String providerId, boolean enabled) {
        jdbc.sql("UPDATE provider_configs SET enabled = :enabled WHERE provider_id = :providerId")
                .param("enabled", enabled)
                .param("providerId", providerId)
                .update();
    }

    private ProviderConfig map(Map<String, Object> row) {
        Timestamp updatedAt = (Timestamp) row.get("updated_at");
        return new ProviderConfig(
                (String) row.get("id"),
                (String) row.get("provider_id"),
                (String) row.get("display_name"),
                (String) row.get("api_base_url"),
                (String) row.get("api_key_encrypted"),
                (Boolean) row.get("enabled"),
                (String) row.get("updated_by"),
                updatedAt != null ? updatedAt.toInstant() : Instant.now()
        );
    }
}
