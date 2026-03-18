package com.chj.aigc.tenantservice.generation;

/**
 * 生成任务状态。
 */
public enum GenerationJobStatus {
    PENDING("pending"),
    RUNNING("running"),
    SUCCEEDED("succeeded"),
    FAILED("failed");

    private final String value;

    GenerationJobStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static GenerationJobStatus fromValue(String value) {
        for (GenerationJobStatus item : values()) {
            if (item.value.equalsIgnoreCase(value)) {
                return item;
            }
        }
        throw new IllegalArgumentException("不支持的任务状态: " + value);
    }
}
