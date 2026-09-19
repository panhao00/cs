package com.crs.web.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 学分统计 DTO。
 * 包含总学分、已修学分、平均 GPA 和按院系学分分布。
 */
@Getter
@Setter
@AllArgsConstructor
public class CreditSummary {

    /** 总学分(所有已修课程的 credits 之和) */
    private int totalCredits;

    /** 已修学分(有成绩的课程学分之和) */
    private int completedCredits;

    /** 平均 GPA */
    private double averageGpa;

    /** 按院系学分分布(院系名 → 学分数) */
    private Map<String, Integer> departmentCredits;
}
