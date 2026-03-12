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
    public List<PaymentOrder> listPaymentOrders(String tenantId) {
        return jdbcTemplate.query(
                """
                select id, tenant_id, channel, status, amount, description, reference_id, qr_code, created_at, paid_at
                from tenant_payment_orders
                where tenant_id = ?
                order by created_at desc
                """,
                this::mapPaymentOrder,
                tenantId
        );
    }

    @Override
    public PaymentOrder findPaymentOrder(String orderId) {
        List<PaymentOrder> orders = jdbcTemplate.query(
                """
                select id, tenant_id, channel, status, amount, description, reference_id, qr_code, created_at, paid_at
                from tenant_payment_orders
                where id = ?
                """,
                this::mapPaymentOrder,
                orderId
        );
        return orders.isEmpty() ? null : orders.getFirst();
    }

    @Override
    public void savePaymentOrder(PaymentOrder order) {
        jdbcTemplate.update(
                """
                insert into tenant_payment_orders (id, tenant_id, channel, status, amount, description, reference_id, qr_code, created_at, paid_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (id) do update set
                    tenant_id = excluded.tenant_id,
                    channel = excluded.channel,
                    status = excluded.status,
                    amount = excluded.amount,
                    description = excluded.description,
                    reference_id = excluded.reference_id,
                    qr_code = excluded.qr_code,
                    created_at = excluded.created_at,
                    paid_at = excluded.paid_at
                """,
                order.id(),
                order.tenantId(),
                order.channel().name(),
                order.status().name(),
                order.amount().amount(),
                order.description(),
                order.referenceId(),
                order.qrCode(),
                Timestamp.from(order.createdAt()),
                order.paidAt() == null ? null : Timestamp.from(order.paidAt())
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

    private PaymentOrder mapPaymentOrder(ResultSet resultSet, int rowNum) throws SQLException {
        Timestamp paidAt = resultSet.getTimestamp("paid_at");
        return new PaymentOrder(
                resultSet.getString("id"),
                resultSet.getString("tenant_id"),
                PaymentChannel.valueOf(resultSet.getString("channel")),
                PaymentOrderStatus.valueOf(resultSet.getString("status")),
                new Money(resultSet.getBigDecimal("amount")),
                resultSet.getString("description"),
                resultSet.getString("reference_id"),
                resultSet.getString("qr_code"),
                resultSet.getTimestamp("created_at").toInstant(),
                paidAt == null ? null : paidAt.toInstant()
        );
    }
}
