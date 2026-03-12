package com.chj.aigc.tenantservice.persistence.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * 租户额度 MyBatis 映射接口。
 */
public interface TenantBillingMapper {
    List<Map<String, Object>> listQuotaAllocations(@Param("tenantId") String tenantId);

    void upsertQuotaAllocation(Map<String, Object> allocation);
}
