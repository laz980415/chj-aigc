package com.chj.aigc.access;

import java.util.Objects;

public record ModelAccessScope(
        AccessScopeType type,
        String value
) {
    public ModelAccessScope {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(value, "value");
    }
}
