package com.chj.aigc.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TenantFinanceServiceTest {

    private final TenantFinanceService financeService = new TenantFinanceService();

    @Test
    void walletTracksRechargeAndUsageBalance() {
        TenantWallet wallet = new TenantWallet("tenant-1");

        financeService.recharge(wallet, "r-1", Money.of("100"), "manual recharge", "order-1");
        financeService.deductUsage(wallet, "u-1", Money.of("8.5"), "image generation", "task-1");

        assertEquals(Money.of("91.5000"), wallet.balance());
        assertEquals(2, wallet.ledgerEntries().size());
    }

    @Test
    void quotaCanBeAssignedToProjectAndConsumed() {
        TenantQuotaBook quotaBook = new TenantQuotaBook("tenant-1");
        QuotaScope projectScope = new QuotaScope(QuotaScopeType.PROJECT, "project-1");
        quotaBook.upsert(new QuotaAllocation(
                "qa-1",
                "tenant-1",
                projectScope,
                QuotaDimension.VIDEO_SECONDS,
                new BigDecimal("300"),
                BigDecimal.ZERO
        ));

        assertTrue(quotaBook.canConsume(new QuotaRequest(
                projectScope,
                QuotaDimension.VIDEO_SECONDS,
                new BigDecimal("45")
        )));

        QuotaAllocation updated = quotaBook.consume(new QuotaRequest(
                projectScope,
                QuotaDimension.VIDEO_SECONDS,
                new BigDecimal("45")
        ));

        assertEquals(new BigDecimal("255"), updated.remaining());
    }

    @Test
    void quotaCanBeAssignedToUserAndCheckedAcrossDimensions() {
        TenantQuotaBook quotaBook = new TenantQuotaBook("tenant-1");
        QuotaScope userScope = new QuotaScope(QuotaScopeType.USER, "user-1");
        quotaBook.upsert(new QuotaAllocation(
                "qa-2",
                "tenant-1",
                userScope,
                QuotaDimension.TOKENS,
                new BigDecimal("100000"),
                new BigDecimal("2500")
        ));
        quotaBook.upsert(new QuotaAllocation(
                "qa-3",
                "tenant-1",
                userScope,
                QuotaDimension.DAILY_REQUESTS,
                new BigDecimal("50"),
                new BigDecimal("49")
        ));

        assertTrue(quotaBook.canConsume(new QuotaRequest(
                userScope,
                QuotaDimension.TOKENS,
                new BigDecimal("1000")
        )));
        assertFalse(quotaBook.canConsume(new QuotaRequest(
                userScope,
                QuotaDimension.DAILY_REQUESTS,
                new BigDecimal("2")
        )));
    }

    @Test
    void consumingBeyondQuotaFails() {
        TenantQuotaBook quotaBook = new TenantQuotaBook("tenant-1");
        QuotaScope userScope = new QuotaScope(QuotaScopeType.USER, "user-2");
        quotaBook.upsert(new QuotaAllocation(
                "qa-4",
                "tenant-1",
                userScope,
                QuotaDimension.IMAGE_COUNT,
                new BigDecimal("10"),
                new BigDecimal("9")
        ));

        assertThrows(IllegalStateException.class, () -> quotaBook.consume(new QuotaRequest(
                userScope,
                QuotaDimension.IMAGE_COUNT,
                new BigDecimal("2")
        )));
    }
}
