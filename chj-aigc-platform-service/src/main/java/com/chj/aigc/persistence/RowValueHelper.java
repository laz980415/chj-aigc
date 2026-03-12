package com.chj.aigc.persistence;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Locale;
import java.util.Map;

/**
 * 统一处理 MyBatis Map 结果集取值，兼容下划线、大小写和别名差异。
 */
public final class RowValueHelper {
    private RowValueHelper() {
    }

    public static Object value(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            if (row.containsKey(key)) {
                return row.get(key);
            }
        }
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String normalized = normalize(entry.getKey());
            for (String key : keys) {
                if (normalized.equals(normalize(key))) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    public static String string(Map<String, Object> row, String... keys) {
        Object value = value(row, keys);
        return value == null ? null : String.valueOf(value);
    }

    public static boolean bool(Map<String, Object> row, String... keys) {
        Object value = value(row, keys);
        return Boolean.TRUE.equals(value);
    }

    public static BigDecimal decimal(Map<String, Object> row, String... keys) {
        return (BigDecimal) value(row, keys);
    }

    public static Timestamp timestamp(Map<String, Object> row, String... keys) {
        return (Timestamp) value(row, keys);
    }

    private static String normalize(String key) {
        return key.replace("_", "").toLowerCase(Locale.ROOT);
    }
}
