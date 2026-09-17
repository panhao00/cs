package com.crs.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 学生选课记录 + 成绩(教授录入成绩界面用)。
 */
@Getter
@Setter
@AllArgsConstructor
public class StudentGradeDTO {
    private Long enrollmentId;
    private Long studentId;
    private String studentName;
    private String grade;  // A/B/C/D/F/I 或 null
    private String status;  // ENROLLED / CANCELLED
}
