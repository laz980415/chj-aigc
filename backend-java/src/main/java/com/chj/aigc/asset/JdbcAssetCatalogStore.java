package com.chj.aigc.asset;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;

public final class JdbcAssetCatalogStore implements AssetCatalogStore {
    private final JdbcTemplate jdbcTemplate;

    public JdbcAssetCatalogStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Client> listClients(String tenantId) {
        return jdbcTemplate.query(
                """
                select id, tenant_id, name, active
                from tenant_clients
                where tenant_id = ?
                order by name asc
                """,
                this::mapClient,
                tenantId
        );
    }

    @Override
    public void saveClient(Client client) {
        jdbcTemplate.update(
                """
                insert into tenant_clients (id, tenant_id, name, active)
                values (?, ?, ?, ?)
                on conflict (id) do update set
                    tenant_id = excluded.tenant_id,
                    name = excluded.name,
                    active = excluded.active
                """,
                client.id(),
                client.tenantId(),
                client.name(),
                client.active()
        );
    }

    @Override
    public List<Brand> listBrands(String tenantId) {
        return jdbcTemplate.query(
                """
                select id, tenant_id, client_id, name, summary, active
                from tenant_brands
                where tenant_id = ?
                order by name asc
                """,
                this::mapBrand,
                tenantId
        );
    }

    @Override
    public void saveBrand(Brand brand) {
        jdbcTemplate.update(
                """
                insert into tenant_brands (id, tenant_id, client_id, name, summary, active)
                values (?, ?, ?, ?, ?, ?)
                on conflict (id) do update set
                    tenant_id = excluded.tenant_id,
                    client_id = excluded.client_id,
                    name = excluded.name,
                    summary = excluded.summary,
                    active = excluded.active
                """,
                brand.id(),
                brand.tenantId(),
                brand.clientId(),
                brand.name(),
                brand.summary(),
                brand.active()
        );
    }

    @Override
    public List<Asset> listAssets(String tenantId) {
        return jdbcTemplate.query(
                """
                select id, tenant_id, project_id, client_id, brand_id, name, kind, uri, tags, active
                from tenant_assets
                where tenant_id = ?
                order by name asc
                """,
                this::mapAsset,
                tenantId
        );
    }

    @Override
    public void saveAsset(Asset asset) {
        jdbcTemplate.update(
                """
                insert into tenant_assets (id, tenant_id, project_id, client_id, brand_id, name, kind, uri, tags, active)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (id) do update set
                    tenant_id = excluded.tenant_id,
                    project_id = excluded.project_id,
                    client_id = excluded.client_id,
                    brand_id = excluded.brand_id,
                    name = excluded.name,
                    kind = excluded.kind,
                    uri = excluded.uri,
                    tags = excluded.tags,
                    active = excluded.active
                """,
                asset.id(),
                asset.tenantId(),
                asset.projectId(),
                asset.clientId(),
                asset.brandId(),
                asset.name(),
                asset.kind().name(),
                asset.uri(),
                String.join(",", asset.tags()),
                asset.active()
        );
    }

    private Client mapClient(ResultSet resultSet, int rowNum) throws SQLException {
        return new Client(
                resultSet.getString("id"),
                resultSet.getString("tenant_id"),
                resultSet.getString("name"),
                resultSet.getBoolean("active")
        );
    }

    private Brand mapBrand(ResultSet resultSet, int rowNum) throws SQLException {
        return new Brand(
                resultSet.getString("id"),
                resultSet.getString("tenant_id"),
                resultSet.getString("client_id"),
                resultSet.getString("name"),
                resultSet.getString("summary"),
                resultSet.getBoolean("active")
        );
    }

    private Asset mapAsset(ResultSet resultSet, int rowNum) throws SQLException {
        String tags = resultSet.getString("tags");
        Set<String> tagSet = tags == null || tags.isBlank()
                ? Set.of()
                : Arrays.stream(tags.split(","))
                        .map(String::trim)
                        .filter(item -> !item.isBlank())
                        .collect(Collectors.toSet());
        return new Asset(
                resultSet.getString("id"),
                resultSet.getString("tenant_id"),
                resultSet.getString("project_id"),
                resultSet.getString("client_id"),
                resultSet.getString("brand_id"),
                resultSet.getString("name"),
                AssetKind.valueOf(resultSet.getString("kind")),
                resultSet.getString("uri"),
                tagSet,
                resultSet.getBoolean("active")
        );
    }
}
