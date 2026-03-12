package com.chj.aigc.tenantservice.billing;

import com.chj.aigc.tenantservice.persistence.RowValueHelper;
import com.chj.aigc.tenantservice.persistence.mapper.TenantBillingMapper;
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
