package com.crs.web.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 成绩单 DTO(学生查看上学期成绩单)。
 * 对应用例 7.5 View Report Card。
 */
@Getter
@Setter
@AllArgsConstructor
public class ReportCardDTO {
    private String semester;
    private List<ReportEntry> entries;
    private double gpa;  // 学期 GPA

    @Getter
    @Setter
    @AllArgsConstructor
    public static class ReportEntry {
        private Long offeringId;
        private String courseId;
        private String courseTitle;
        private String department;
        private Integer credits;
        private String grade;
        private String professorName;
        private String maskedSsn;
    }
}
