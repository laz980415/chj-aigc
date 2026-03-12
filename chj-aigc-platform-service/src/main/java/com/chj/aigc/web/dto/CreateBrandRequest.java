package com.chj.aigc.web.dto;

public record CreateBrandRequest(
        String brandId,
        String clientId,
        String name,
        String summary
) {
}
