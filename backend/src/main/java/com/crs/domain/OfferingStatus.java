package com.crs.domain;

/**
 * 课程开设状态(对应需求 9.5 节状态机)。
 * <ul>
 *   <li>OPEN      —— 开放选课</li>
 *   <li>COMMITTED —— 已提交(达达标:有教授且≥3人)</li>
 *   <li>CLOSED    —— 注册已关闭</li>
 *   <li>CANCELLED —— 取消(人数不足3 或 无教授)</li>
 * </ul>
 */
public enum OfferingStatus {
    OPEN,
    COMMITTED,
    CLOSED,
    CANCELLED
}
