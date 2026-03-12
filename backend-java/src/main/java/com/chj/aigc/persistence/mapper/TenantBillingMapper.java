package com.chj.aigc.persistence.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * 租户钱包流水和额度分配的 MyBatis 映射接口。
 */
public interface TenantBillingMapper {
    List<Map<String, Object>> listLedgerEntries(@Param("tenantId") String tenantId);

    void upsertLedgerEntry(Map<String, Object> entry);

    List<Map<String, Object>> listPaymentOrders(@Param("tenantId") String tenantId);

    Map<String, Object> findPaymentOrder(@Param("orderId") String orderId);

    void upsertPaymentOrder(Map<String, Object> order);

    List<Map<String, Object>> listQuotaAllocations(@Param("tenantId") String tenantId);

    void upsertQuotaAllocation(Map<String, Object> allocation);
}
