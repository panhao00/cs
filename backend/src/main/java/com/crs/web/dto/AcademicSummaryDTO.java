package com.crs.web.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 学业摘要 DTO: 返回学生的累计 GPA、学分统计和按学期分组的课程历史。
 * <p>
 * 包含:
 * - 累计 GPA(所有有成绩的学期)
 * - 已获得学分(成绩为 A/B/C/D 的课程)
 * - 已修读学分(所有有成绩的课程,含 F/I)
 * - 当前学期选课学分
 * - 按学期分组的课程历史(逆序: 最新学期在前)
 */
@Getter
@Setter
@AllArgsConstructor
public class AcademicSummaryDTO {

    /** 累计 GPA(所有有成绩学期的加权平均) */
    private double cumulativeGpa;

    /** 已获得学分(成绩为 A/B/C/D 的课程学分总和) */
    private int totalCreditsEarned;

    /** 已修读学分(所有有成绩的课程学分总和,含 F/I) */
    private int totalCreditsAttempted;

    /** 当前学期选课学分 */
    private int currentEnrolledCredits;

    /** 按学期分组的课程历史(逆序: 最新学期在前) */
    private List<SemesterEntry> semesterHistory;

    /**
     * 单个学期的成绩条目。
     */
    @Getter
    @Setter
    @AllArgsConstructor
    public static class SemesterEntry {

        /** 学期标识,如 "2026-FALL" */
        private String semester;

        /** 该学期 GPA */
        private double semesterGpa;

        /** 该学期总学分 */
        private int credits;

        /** 该学期课程成绩列表 */
        private List<CourseGrade> courses;
    }

    /**
     * 单门课程的成绩信息。
     */
    @Getter
    @Setter
    @AllArgsConstructor
    public static class CourseGrade {

        /** 课程编号 */
        private String courseId;

        /** 课程名称 */
        private String courseTitle;

        /** 课程学分 */
        private int credits;

        /** 成绩等级(A/B/C/D/F/I) */
        private String grade;

        /** 质量点(绩点 × 学分) */
        private double qualityPoints;
    }
}
