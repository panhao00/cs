package com.crs.web.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 班级成绩统计 DTO。
 * <p>
 * 教授查看某开设的成绩分布概况,包括总人数、已录入/未录入人数、
 * 平均绩点、各等级人数分布、最高/最低成绩等。
 */
@Getter
@Setter
@AllArgsConstructor
public class GradeStatisticsDTO {

    private Long offeringId;
    private String courseId;
    private String courseTitle;
    private int totalStudents;
    private int gradedStudents;
    private int ungradedStudents;
    /** 平均绩点(A=4,B=3,C=2,D=1,F=0,I 不计入) */
    private Double averageGrade;
    /** 各等级人数分布,如 {"A":3,"B":5,"C":2,"D":0,"F":1} */
    private Map<String, Integer> gradeDistribution;
    private String highestGrade;
    private String lowestGrade;
}
