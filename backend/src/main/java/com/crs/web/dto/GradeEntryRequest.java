package com.crs.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 录入/修改单个学生成绩请求 DTO。
 * 对应用例 7.4 Submit Grades: 教授为每生录入成绩 A/B/C/D/F/I,可留空补录,可修改。
 */
@Getter
@Setter
public class GradeEntryRequest {

    @NotNull(message = "studentId 不能为空")
    private Long studentId;

    /**
     * 成绩: A/B/C/D/F/I,或 null(留空稍后补录)。
     * 当 clear=true 时 grade 可为 null(清除成绩)。
     */
    private String grade;

    /** true=清除成绩(设为 null) */
    private boolean clear;
}
