package com.crs.domain;

/**
 * 课表项状态。
 * <ul>
 *   <li>SELECTED    —— 已选择但未正式注册</li>
 *   <li>ENROLLED_IN —— 已正式注册</li>
 *   <li>CANCELLED   —— 已取消(开设被取消)</li>
 * </ul>
 */
public enum ItemStatus {
    SELECTED,
    ENROLLED_IN,
    CANCELLED
}
