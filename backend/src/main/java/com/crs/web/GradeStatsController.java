package com.crs.web;

import com.crs.service.GradeStatsService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.crs.web.dto.CreditSummary;
import com.crs.web.dto.GpaTrend;

/**
 * 成绩统计控制器: GPA 趋势、学分统计、成绩分布。
 * <p>
 * 端点:
 * - GET /stats/gpa-trend                     学生查看 GPA 趋势(需 STUDENT)
 * - GET /stats/credit-summary                学生查看学分统计(需 STUDENT)
 * - GET /stats/grade-distribution/{offeringId} 教授查看成绩分布(需 PROFESSOR)
 */
@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class GradeStatsController {

    private final GradeStatsService gradeStatsService;
    private final RoleGuard roleGuard;

    /**
     * 获取当前学生所有学期的 GPA 趋势。
     */
    @GetMapping("/gpa-trend")
    public ResponseEntity<List<GpaTrend>> getGpaTrend() {
        Long studentId = roleGuard.requireStudent();
        return ResponseEntity.ok(gradeStatsService.getGpaTrend(studentId));
    }

    /**
     * 获取当前学生的学分统计。
     */
    @GetMapping("/credit-summary")
    public ResponseEntity<CreditSummary> getCreditSummary() {
        Long studentId = roleGuard.requireStudent();
        return ResponseEntity.ok(gradeStatsService.getCreditSummary(studentId));
    }

    /**
     * 获取某开设的成绩分布(教授查看班级成绩分布)。
     */
    @GetMapping("/grade-distribution/{offeringId}")
    public ResponseEntity<Map<String, Object>> getGradeDistribution(
            @PathVariable Long offeringId) {
        roleGuard.requireProfessor();
        return ResponseEntity.ok(gradeStatsService.getGradeDistribution(offeringId));
    }
}
