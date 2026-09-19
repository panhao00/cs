package com.crs.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * GPA 趋势数据 DTO。
 * 每条记录代表一个学期的 GPA、学分和课程数量。
 */
@Getter
@Setter
@AllArgsConstructor
public class GpaTrend {

    /** 学期,如 "2026-SPRING" */
    private String semester;

    /** 该学期 GPA(加权平均) */
    private double gpa;

    /** 该学期学分总和 */
    private int credits;

    /** 该学期课程数量 */
    private int courseCount;
}
