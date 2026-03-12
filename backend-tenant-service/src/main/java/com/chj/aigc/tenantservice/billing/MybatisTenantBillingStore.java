package com.chj.aigc.tenantservice.billing;

import com.chj.aigc.tenantservice.persistence.RowValueHelper;
import com.chj.aigc.tenantservice.persistence.mapper.TenantBillingMapper;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 基于 MyBatis XML 的租户额度存储。
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
                .filter(Objects::nonNull)
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
                .filter(Objects::nonNull)
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
                .filter(Objects::nonNull)
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

    private QuotaAllocation mapQuotaAllocation(Map<String, Object> row) {
        QuotaScopeType scopeType = parseScopeType(RowValueHelper.string(row, "scopeType", "scope_type"));
        QuotaDimension dimension = parseDimension(RowValueHelper.string(row, "dimension"));
        if (scopeType == null || dimension == null) {
            return null;
        }
        return new QuotaAllocation(
                RowValueHelper.string(row, "id"),
                RowValueHelper.string(row, "tenantId", "tenant_id"),
                new QuotaScope(
                        scopeType,
                        RowValueHelper.string(row, "scopeId", "scope_id")
                ),
                dimension,
                RowValueHelper.decimal(row, "limitValue", "limit_value"),
                RowValueHelper.decimal(row, "usedValue", "used_value")
        );
    }

    private WalletLedgerEntry mapLedgerEntry(Map<String, Object> row) {
        Timestamp createdAt = RowValueHelper.timestamp(row, "createdAt", "created_at");
        if (createdAt == null) {
            return null;
        }
        String entryType = RowValueHelper.string(row, "entryType", "entry_type");
        if (entryType == null) {
            return null;
        }
        return new WalletLedgerEntry(
                RowValueHelper.string(row, "id"),
                RowValueHelper.string(row, "tenantId", "tenant_id"),
                LedgerEntryType.valueOf(entryType),
                new Money(RowValueHelper.decimal(row, "amount")),
                RowValueHelper.string(row, "description"),
                RowValueHelper.string(row, "referenceId", "reference_id"),
                createdAt.toInstant()
        );
    }

    private PaymentOrder mapPaymentOrder(Map<String, Object> row) {
        Timestamp createdAt = RowValueHelper.timestamp(row, "createdAt", "created_at");
        if (createdAt == null) {
            return null;
        }
        Timestamp paidAt = RowValueHelper.timestamp(row, "paidAt", "paid_at");
        String channel = RowValueHelper.string(row, "channel");
        String status = RowValueHelper.string(row, "status");
        if (channel == null || status == null) {
            return null;
        }
        return new PaymentOrder(
                RowValueHelper.string(row, "id"),
                RowValueHelper.string(row, "tenantId", "tenant_id"),
                PaymentChannel.valueOf(channel),
                PaymentOrderStatus.valueOf(status),
                new Money(RowValueHelper.decimal(row, "amount")),
                RowValueHelper.string(row, "description"),
                RowValueHelper.string(row, "referenceId", "reference_id"),
                RowValueHelper.string(row, "qrCode", "qr_code"),
                createdAt.toInstant(),
                paidAt == null ? null : paidAt.toInstant()
        );
    }

    private QuotaScopeType parseScopeType(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        return switch (rawValue.trim().toUpperCase()) {
            case "PROJECT" -> QuotaScopeType.PROJECT;
            case "USER", "MEMBER" -> QuotaScopeType.USER;
            default -> null;
        };
    }

    private QuotaDimension parseDimension(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        return switch (rawValue.trim().toUpperCase()) {
            case "TOKENS", "INPUT_TOKENS", "OUTPUT_TOKENS" -> QuotaDimension.TOKENS;
            case "IMAGE_COUNT", "IMAGES" -> QuotaDimension.IMAGE_COUNT;
            case "VIDEO_SECONDS" -> QuotaDimension.VIDEO_SECONDS;
            default -> null;
        };
    }
}
