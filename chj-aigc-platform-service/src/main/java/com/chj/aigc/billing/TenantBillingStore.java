package com.chj.aigc.billing;

import java.util.List;

public interface TenantBillingStore {
    List<WalletLedgerEntry> listLedgerEntries(String tenantId);

    void saveLedgerEntry(WalletLedgerEntry entry);

    List<PaymentOrder> listPaymentOrders(String tenantId);

    PaymentOrder findPaymentOrder(String orderId);

    void savePaymentOrder(PaymentOrder order);

    List<QuotaAllocation> listQuotaAllocations(String tenantId);

    void saveQuotaAllocation(QuotaAllocation allocation);
}
