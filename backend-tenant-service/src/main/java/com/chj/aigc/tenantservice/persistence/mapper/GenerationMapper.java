package com.chj.aigc.tenantservice.persistence.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * 生成任务 MyBatis Mapper。
 */
public interface GenerationMapper {
    List<Map<String, Object>> listJobs(@Param("tenantId") String tenantId);

    Map<String, Object> findJob(@Param("jobId") String jobId);

    void upsertJob(Map<String, Object> payload);
}
