package com.chj.aigc.tenantservice.billing;

import java.util.List;

/**
 * 租户额度存储接口。
 */
public interface TenantBillingStore {
    List<WalletLedgerEntry> listLedgerEntries(String tenantId);

    void saveLedgerEntry(WalletLedgerEntry entry);

    List<PaymentOrder> listPaymentOrders(String tenantId);

    PaymentOrder findPaymentOrder(String orderId);

    void savePaymentOrder(PaymentOrder order);

    List<QuotaAllocation> listQuotaAllocations(String tenantId);

    void saveQuotaAllocation(QuotaAllocation allocation);
}
