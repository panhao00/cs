package com.crs.web.dto;

import com.crs.domain.OfferingStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 教授视图的课程开设 DTO(教授选课授课用例用)。
 */
@Getter
@Setter
@AllArgsConstructor
public class TeachingOfferingDTO {
    private Long offeringId;
    private String courseId;
    private String courseTitle;
    private String department;
    private String semester;
    private String days;
    private String times;
    private String room;
    private OfferingStatus status;
    /** 当前教授是否已选择授课此开设 */
    private boolean selectedByMe;
    /** 已注册学生数 */
    private int enrolledCount;
    private int maxCapacity;
}
