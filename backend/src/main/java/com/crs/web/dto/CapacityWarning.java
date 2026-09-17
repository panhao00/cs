package com.crs.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 容量警告 DTO: 在学生构建课表期间,若课程被填满则提前通知。
 * <p>
 * 用于 selectOfferings 响应中,提示学生所选课程的容量变化。
 */
@Getter
@Setter
@AllArgsConstructor
public class CapacityWarning {
    private Long offeringId;
    private String courseId;
    private int currentEnrolled;
    private int maxCapacity;
    private String message;
}
