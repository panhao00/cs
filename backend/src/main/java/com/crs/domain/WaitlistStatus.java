package com.crs.domain;

/**
 * 候补名单状态。
 * <ul>
 *   <li>WAITING   —— 等待递补</li>
 *   <li>ENROLLED  —— 已被递补注册</li>
 *   <li>CANCELLED —— 学生主动取消</li>
 * </ul>
 */
public enum WaitlistStatus {
    WAITING,
    ENROLLED,
    CANCELLED
}
