package com.crs.domain;

/**
 * 课表项类型: 主选 / 备选。
 * 对应业务规则: 每学期 4 主选 + 2 备选。
 */
public enum ItemKind {
    PRIMARY,
    ALTERNATE
}
