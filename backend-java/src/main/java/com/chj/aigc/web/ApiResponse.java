package com.chj.aigc.web;

/**
 * 统一接口返回体。
 * 前端只需要解析 code、message 和 data，便于后续增加全局错误处理和埋点。
 */
public record ApiResponse<T>(
        int code,
        String message,
        T data
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data);
    }

    public static ApiResponse<Void> success() {
        return success(null);
    }

    public static ApiResponse<Void> failure(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
