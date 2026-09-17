package com.crs.web.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 关闭注册结果 DTO(对应用例 7.8 Close Registration)。
 * <p>
 * 包含:
 * - 成功/失败状态
 * - 满员开设列表(满足最小人数要求)
 * - 未满开设列表(不满足最小人数要求,需通知学生)
 * - 通知 Billing 系统状态
 */
@Getter
@Setter
@AllArgsConstructor
public class CloseRegistrationResult {
    private boolean success;
    private String semester;
    private String message;
    private int totalOfferings;
    private int fullOfferings;  // 满足最小人数
    private int underFilledOfferings;  // 未满
    private List<OfferingStatus> offerings;
    private boolean billingNotificationSent;

    @Getter
    @Setter
    @AllArgsConstructor
    public static class OfferingStatus {
        private Long offeringId;
        private String courseId;
        private int enrolledCount;
        private int minRequired;
        private boolean meetsMinimum;
        private String message;  // "满员通知" 或 "未满,需通知学生"
    }
}
