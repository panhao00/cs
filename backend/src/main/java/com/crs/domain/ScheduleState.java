package com.crs.domain;

/**
 * 课表状态。
 * <ul>
 *   <li>DRAFT     —— 草稿(创建/更新中)</li>
 *   <li>COMMITTED —— 已提交</li>
 *   <li>CLOSED    —— 注册已关闭</li>
 * </ul>
 */
public enum ScheduleState {
    DRAFT,
    COMMITTED,
    CLOSED
}
