package com.chj.aigc.web.dto;

/**
 * 创建模拟微信支付订单请求。
 */
public record CreatePaymentOrderRequest(
        String orderId,
        String tenantId,
        String amount,
        String description,
        String referenceId
) {
}
