package com.crs.web;

import com.crs.service.ReviewService;
import com.crs.web.dto.ReviewDTO;
import com.crs.web.dto.ReviewSubmitRequest;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 课程评价控制器。
 * <p>
 * 端点:
 * - POST   /reviews              提交评价(学生)
 * - PUT    /reviews/{id}         修改评价(学生)
 * - DELETE /reviews/{id}         删除评价(学生)
 * - GET    /reviews/offering/{offeringId}  查看某开设评价
 * - GET    /reviews/me           我的评价列表
 * - GET    /reviews/offering/{offeringId}/average  某开设平均评分
 * - GET    /reviews/course/{courseId}/average      课程综合平均评分
 * <p>
 * 安全: 提交/修改/删除需 STUDENT 角色; 查看需登录。
 */
@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final RoleGuard roleGuard;

    /**
     * 提交评价。
     */
    @PostMapping
    public ResponseEntity<ReviewDTO> submitReview(
            @RequestParam Long offeringId,
            @Valid @RequestBody ReviewSubmitRequest request) {
        Long studentId = roleGuard.requireStudent();
        ReviewDTO dto = reviewService.submitReview(studentId, offeringId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * 修改评价。
     */
    @PutMapping("/{id}")
    public ResponseEntity<ReviewDTO> updateReview(
            @PathVariable Long id,
            @Valid @RequestBody ReviewSubmitRequest request) {
        Long studentId = roleGuard.requireStudent();
        ReviewDTO dto = reviewService.updateReview(studentId, id, request);
        return ResponseEntity.ok(dto);
    }

    /**
     * 删除评价。
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        Long studentId = roleGuard.requireStudent();
        reviewService.deleteReview(studentId, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 查看某开设的所有评价。
     */
    @GetMapping("/offering/{offeringId}")
    public ResponseEntity<List<ReviewDTO>> getOfferingReviews(@PathVariable Long offeringId) {
        roleGuard.requireAnyLogin();
        return ResponseEntity.ok(reviewService.getOfferingReviews(offeringId));
    }

    /**
     * 查看某开设的平均评分。
     */
    @GetMapping("/offering/{offeringId}/average")
    public ResponseEntity<Map<String, Object>> getOfferingAverage(@PathVariable Long offeringId) {
        roleGuard.requireAnyLogin();
        Map<String, Object> result = new HashMap<>();
        result.put("offeringId", offeringId);
        reviewService.getOfferingAverageRating(offeringId)
                .ifPresentOrElse(
                        avg -> result.put("averageRating", avg),
                        () -> {
                            result.put("averageRating", 0);
                            result.put("message", "暂无评价");
                        });
        return ResponseEntity.ok(result);
    }

    /**
     * 获取课程综合平均评分。
     */
    @GetMapping("/course/{courseId}/average")
    public ResponseEntity<Map<String, Object>> getCourseAverage(@PathVariable String courseId) {
        roleGuard.requireAnyLogin();
        Map<String, Object> result = new HashMap<>();
        result.put("courseId", courseId);
        reviewService.getCourseAverageRating(courseId)
                .ifPresentOrElse(
                        avg -> result.put("averageRating", avg),
                        () -> {
                            result.put("averageRating", 0);
                            result.put("message", "暂无评价");
                        });
        return ResponseEntity.ok(result);
    }

    /**
     * 获取我的评价列表。
     */
    @GetMapping("/me")
    public ResponseEntity<List<ReviewDTO>> getMyReviews() {
        Long studentId = roleGuard.requireStudent();
        return ResponseEntity.ok(reviewService.getMyReviews(studentId));
    }
}
