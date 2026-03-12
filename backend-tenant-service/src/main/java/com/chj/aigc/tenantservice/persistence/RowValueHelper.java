package com.chj.aigc.tenantservice.persistence;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Map;

/**
 * 统一处理 MyBatis 返回 Map 的字段读取。
 */
public final class RowValueHelper {
    private RowValueHelper() {
    }

    public static String string(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            Object value = value(row, key);
            if (value != null) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    public static boolean bool(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            Object value = value(row, key);
            if (value instanceof Boolean booleanValue) {
                return booleanValue;
            }
            if (value != null) {
                return Boolean.parseBoolean(String.valueOf(value));
            }
        }
        return false;
    }

    public static BigDecimal decimal(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            Object value = value(row, key);
            if (value instanceof BigDecimal decimalValue) {
                return decimalValue;
            }
            if (value != null) {
                return new BigDecimal(String.valueOf(value));
            }
        }
        return BigDecimal.ZERO;
    }

    public static Timestamp timestamp(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            Object value = value(row, key);
            if (value instanceof Timestamp timestamp) {
                return timestamp;
            }
            if (value != null) {
                return Timestamp.valueOf(String.valueOf(value).replace("T", " ").replace("Z", ""));
            }
        }
        return null;
    }

    private static Object value(Map<String, Object> row, String key) {
        if (row.containsKey(key)) {
            return row.get(key);
        }
        String normalizedKey = normalize(key);
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (normalize(entry.getKey()).equals(normalizedKey)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static String normalize(String key) {
        return key == null ? "" : key.replace("_", "").toLowerCase();
    }
}
