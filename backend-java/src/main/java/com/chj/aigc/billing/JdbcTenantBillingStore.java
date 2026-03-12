package com.chj.aigc.billing;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 基于 PostgreSQL 的租户钱包和额度存储实现。
 */
public final class JdbcTenantBillingStore implements TenantBillingStore {
    private final JdbcTemplate jdbcTemplate;

    public JdbcTenantBillingStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<WalletLedgerEntry> listLedgerEntries(String tenantId) {
        return jdbcTemplate.query(
                """
                select id, tenant_id, entry_type, amount, description, reference_id, created_at
                from tenant_wallet_ledger
                where tenant_id = ?
                order by created_at asc
                """,
                this::mapLedgerEntry,
                tenantId
        );
    }

    @Override
    public void saveLedgerEntry(WalletLedgerEntry entry) {
        jdbcTemplate.update(
                """
                insert into tenant_wallet_ledger (id, tenant_id, entry_type, amount, description, reference_id, created_at)
                values (?, ?, ?, ?, ?, ?, ?)
                on conflict (id) do nothing
                """,
                entry.id(),
                entry.tenantId(),
                entry.type().name(),
                entry.amount().amount(),
                entry.description(),
                entry.referenceId(),
                Timestamp.from(entry.createdAt())
        );
    }

    @Override
    public List<QuotaAllocation> listQuotaAllocations(String tenantId) {
        return jdbcTemplate.query(
                """
                select id, tenant_id, scope_type, scope_id, dimension, limit_value, used_value
                from tenant_quota_allocations
                where tenant_id = ?
                order by id asc
                """,
                this::mapQuotaAllocation,
                tenantId
        );
    }

    @Override
    public void saveQuotaAllocation(QuotaAllocation allocation) {
        jdbcTemplate.update(
                """
                insert into tenant_quota_allocations (id, tenant_id, scope_type, scope_id, dimension, limit_value, used_value)
                values (?, ?, ?, ?, ?, ?, ?)
                on conflict (id) do update set
                    tenant_id = excluded.tenant_id,
                    scope_type = excluded.scope_type,
                    scope_id = excluded.scope_id,
                    dimension = excluded.dimension,
                    limit_value = excluded.limit_value,
                    used_value = excluded.used_value
                """,
                allocation.id(),
                allocation.tenantId(),
                allocation.scope().scopeType().name(),
                allocation.scope().scopeId(),
                allocation.dimension().name(),
                allocation.limit(),
                allocation.used()
        );
    }

    private WalletLedgerEntry mapLedgerEntry(ResultSet resultSet, int rowNum) throws SQLException {
        return new WalletLedgerEntry(
                resultSet.getString("id"),
                resultSet.getString("tenant_id"),
                LedgerEntryType.valueOf(resultSet.getString("entry_type")),
                new Money(resultSet.getBigDecimal("amount")),
                resultSet.getString("description"),
                resultSet.getString("reference_id"),
                resultSet.getTimestamp("created_at").toInstant()
        );
    }

    private QuotaAllocation mapQuotaAllocation(ResultSet resultSet, int rowNum) throws SQLException {
        return new QuotaAllocation(
                resultSet.getString("id"),
                resultSet.getString("tenant_id"),
                new QuotaScope(
                        QuotaScopeType.valueOf(resultSet.getString("scope_type")),
                        resultSet.getString("scope_id")
                ),
                QuotaDimension.valueOf(resultSet.getString("dimension")),
                resultSet.getBigDecimal("limit_value"),
                resultSet.getBigDecimal("used_value")
        );
    }
}
