package com.chj.aigc.tenantservice.generation;

import java.util.List;
import java.util.Optional;

/**
 * 生成任务存储接口。
 */
public interface GenerationStore {
    List<GenerationJob> listJobs(String tenantId);

    Optional<GenerationJob> findJob(String jobId);

    void saveJob(GenerationJob job);
}
