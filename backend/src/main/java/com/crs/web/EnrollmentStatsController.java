package com.crs.web;

import com.crs.service.EnrollmentStatsService;
import com.crs.web.dto.EnrollmentTrend;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 选课统计控制器。
 * <p>
 * 提供热度仪表盘所需的统计数据 REST API。
 */
@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class EnrollmentStatsController {

    private final EnrollmentStatsService enrollmentStatsService;
    private final RoleGuard roleGuard;

    /**
     * 学期总览统计。
     *
     * @param semester 学期
     * @return 总课程数、总学生数、总选课数、平均选课率
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getSemesterOverview(
            @RequestParam String semester) {
        roleGuard.requireAnyLogin();
        return ResponseEntity.ok(enrollmentStatsService.getSemesterOverview(semester));
    }

    /**
     * 课程热度排名。
     *
     * @param semester 学期
     * @return 按选课率降序排列的热度列表
     */
    @GetMapping("/popularity")
    public ResponseEntity<List<EnrollmentTrend>> getOfferingPopularity(
            @RequestParam String semester) {
        roleGuard.requireAnyLogin();
        return ResponseEntity.ok(enrollmentStatsService.getOfferingPopularity(semester));
    }

    /**
     * 课程选课趋势。
     *
     * @param courseId 课程编号
     * @return 该课程各学期的选课人数趋势
     */
    @GetMapping("/trend/{courseId}")
    public ResponseEntity<List<EnrollmentTrend>> getCoursePopularityTrend(
            @PathVariable String courseId) {
        roleGuard.requireAnyLogin();
        return ResponseEntity.ok(enrollmentStatsService.getCoursePopularityTrend(courseId));
    }

    /**
     * 院系选课统计。
     *
     * @param semester 学期
     * @return 各院系选课统计
     */
    @GetMapping("/department")
    public ResponseEntity<List<Map<String, Object>>> getDepartmentEnrollmentStats(
            @RequestParam String semester) {
        roleGuard.requireAnyLogin();
        return ResponseEntity.ok(enrollmentStatsService.getDepartmentEnrollmentStats(semester));
    }
}
