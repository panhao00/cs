package com.crs.web;

import com.crs.service.RecommendationService;
import com.crs.web.dto.CourseRecommendation;
import com.crs.web.RoleGuard;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 选课推荐控制器。
 * <p>
 * 提供个性化课程推荐 REST API。
 */
@RestController
@RequestMapping("/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final RoleGuard roleGuard;

    /**
     * 获取个性化课程推荐。
     * <p>
     * 仅学生可访问,基于先修课程链、专业匹配、热门推荐、学分平衡四个维度。
     *
     * @param semester 当前学期(如 "2026-FALL")
     * @return 按推荐分数降序排列的推荐列表
     */
    @GetMapping
    public ResponseEntity<List<CourseRecommendation>> getRecommendations(
            @RequestParam String semester) {
        Long studentId = roleGuard.requireStudent();
        return ResponseEntity.ok(recommendationService.getRecommendations(studentId, semester));
    }
}
