package com.chj.aigc.billing;

import com.chj.aigc.persistence.RowValueHelper;
import com.chj.aigc.persistence.mapper.TenantBillingMapper;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 MyBatis XML 的租户资金与额度存储实现。
 */
public final class MybatisTenantBillingStore implements TenantBillingStore {
    private final TenantBillingMapper mapper;

    public MybatisTenantBillingStore(TenantBillingMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<WalletLedgerEntry> listLedgerEntries(String tenantId) {
        return mapper.listLedgerEntries(tenantId).stream()
                .map(this::mapLedgerEntry)
                .toList();
    }

    @Override
    public void saveLedgerEntry(WalletLedgerEntry entry) {
        mapper.upsertLedgerEntry(Map.of(
                "id", entry.id(),
                "tenantId", entry.tenantId(),
                "entryType", entry.type().name(),
                "amount", entry.amount().amount(),
                "description", entry.description(),
                "referenceId", entry.referenceId(),
                "createdAt", Timestamp.from(entry.createdAt())
        ));
    }

    @Override
    public List<PaymentOrder> listPaymentOrders(String tenantId) {
        return mapper.listPaymentOrders(tenantId).stream()
                .map(this::mapPaymentOrder)
                .toList();
    }

    @Override
    public PaymentOrder findPaymentOrder(String orderId) {
        Map<String, Object> row = mapper.findPaymentOrder(orderId);
        if (row == null || row.isEmpty()) {
            return null;
        }
        return mapPaymentOrder(row);
    }

    @Override
    public void savePaymentOrder(PaymentOrder order) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", order.id());
        payload.put("tenantId", order.tenantId());
        payload.put("channel", order.channel().name());
        payload.put("status", order.status().name());
        payload.put("amount", order.amount().amount());
        payload.put("description", order.description());
        payload.put("referenceId", order.referenceId());
        payload.put("qrCode", order.qrCode());
        payload.put("createdAt", Timestamp.from(order.createdAt()));
        payload.put("paidAt", order.paidAt() == null ? null : Timestamp.from(order.paidAt()));
        mapper.upsertPaymentOrder(payload);
    }

    @Override
    public List<QuotaAllocation> listQuotaAllocations(String tenantId) {
        return mapper.listQuotaAllocations(tenantId).stream()
                .map(this::mapQuotaAllocation)
                .toList();
    }

    @Override
    public void saveQuotaAllocation(QuotaAllocation allocation) {
        mapper.upsertQuotaAllocation(Map.of(
                "id", allocation.id(),
                "tenantId", allocation.tenantId(),
                "scopeType", allocation.scope().scopeType().name(),
                "scopeId", allocation.scope().scopeId(),
                "dimension", allocation.dimension().name(),
                "limitValue", allocation.limit(),
                "usedValue", allocation.used()
        ));
    }

    private WalletLedgerEntry mapLedgerEntry(Map<String, Object> row) {
        return new WalletLedgerEntry(
                RowValueHelper.string(row, "id"),
                RowValueHelper.string(row, "tenantId", "tenant_id"),
                LedgerEntryType.valueOf(RowValueHelper.string(row, "entryType", "entry_type")),
                new Money(RowValueHelper.decimal(row, "amount")),
                RowValueHelper.string(row, "description"),
                RowValueHelper.string(row, "referenceId", "reference_id"),
                RowValueHelper.timestamp(row, "createdAt", "created_at").toInstant()
        );
    }

    private QuotaAllocation mapQuotaAllocation(Map<String, Object> row) {
        return new QuotaAllocation(
                RowValueHelper.string(row, "id"),
                RowValueHelper.string(row, "tenantId", "tenant_id"),
                new QuotaScope(
                        QuotaScopeType.valueOf(RowValueHelper.string(row, "scopeType", "scope_type")),
                        RowValueHelper.string(row, "scopeId", "scope_id")
                ),
                QuotaDimension.valueOf(RowValueHelper.string(row, "dimension")),
                RowValueHelper.decimal(row, "limitValue", "limit_value"),
                RowValueHelper.decimal(row, "usedValue", "used_value")
        );
    }

    private PaymentOrder mapPaymentOrder(Map<String, Object> row) {
        Timestamp paidAt = RowValueHelper.timestamp(row, "paidAt", "paid_at");
        return new PaymentOrder(
                RowValueHelper.string(row, "id"),
                RowValueHelper.string(row, "tenantId", "tenant_id"),
                PaymentChannel.valueOf(RowValueHelper.string(row, "channel")),
                PaymentOrderStatus.valueOf(RowValueHelper.string(row, "status")),
                new Money(RowValueHelper.decimal(row, "amount")),
                RowValueHelper.string(row, "description"),
                RowValueHelper.string(row, "referenceId", "reference_id"),
                RowValueHelper.string(row, "qrCode", "qr_code"),
                RowValueHelper.timestamp(row, "createdAt", "created_at").toInstant(),
                paidAt == null ? null : paidAt.toInstant()
        );
    }
}
