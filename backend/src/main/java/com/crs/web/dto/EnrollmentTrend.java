package com.crs.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 选课趋势数据 DTO。
 * <p>
 * 用于热度仪表盘展示课程各学期的选课人数变化趋势。
 */
@Getter
@Setter
@AllArgsConstructor
public class EnrollmentTrend {

    /** 课程编号 */
    private String courseId;

    /** 课程名称 */
    private String courseTitle;

    /** 所属院系 */
    private String department;

    /** 学期 */
    private String semester;

    /** 已选课人数 */
    private int enrolledCount;

    /** 最大容量 */
    private int maxCapacity;

    /** 选课率 (enrolledCount / maxCapacity) */
    private double popularityRate;
}
