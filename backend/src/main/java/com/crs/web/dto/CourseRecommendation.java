package com.crs.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 课程推荐结果 DTO。
 * <p>
 * 包含推荐的课程开设信息、推荐分数和推荐理由。
 * 推荐算法基于四个维度: 先修课程链、专业匹配、热门推荐、学分平衡。
 */
@Getter
@Setter
@AllArgsConstructor
public class CourseRecommendation {

    /** 课程开设详细信息 */
    private CourseOfferingDTO offering;

    /** 该课程的平均评分(暂为 0,后续可扩展) */
    private double averageRating;

    /** 当前已选课人数 */
    private int enrolledCount;

    /** 最大容量 */
    private int maxCapacity;

    /** 推荐理由(中文描述) */
    private String reason;

    /** 推荐综合分数(越高越推荐) */
    private double score;
}
