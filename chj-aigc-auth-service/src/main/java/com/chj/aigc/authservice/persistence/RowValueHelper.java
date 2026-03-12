package com.chj.aigc.authservice.persistence;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 认证服务行值读取工具。
 */
public final class RowValueHelper {
    private RowValueHelper() {
    }

    public static String string(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            Object value = row.get(key);
            if (value != null) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    public static Timestamp timestamp(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            Object value = row.get(key);
            if (value instanceof Timestamp timestamp) {
                return timestamp;
            }
            if (value instanceof LocalDateTime localDateTime) {
                return Timestamp.valueOf(localDateTime);
            }
            if (value instanceof java.util.Date date) {
                return new Timestamp(date.getTime());
            }
        }
        return null;
    }

    public static boolean bool(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            Object value = row.get(key);
            if (value instanceof Boolean bool) {
                return bool;
            }
        }
        return false;
    }
}
