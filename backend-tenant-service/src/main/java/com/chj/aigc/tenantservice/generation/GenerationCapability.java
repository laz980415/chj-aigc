package com.chj.aigc.tenantservice.generation;

/**
 * 当前支持的生成能力。
 */
public enum GenerationCapability {
    COPYWRITING("copywriting"),
    IMAGE_GENERATION("image_generation"),
    VIDEO_GENERATION("video_generation");

    private final String value;

    GenerationCapability(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static GenerationCapability fromValue(String value) {
        for (GenerationCapability item : values()) {
            if (item.value.equalsIgnoreCase(value)) {
                return item;
            }
        }
        throw new IllegalArgumentException("不支持的生成能力: " + value);
    }
}
