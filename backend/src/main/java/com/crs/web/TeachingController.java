package com.crs.web;

import com.crs.service.TeachingService;
import com.crs.web.dto.SelectTeachingRequest;
import com.crs.web.dto.TeachingOfferingDTO;
import com.crs.web.dto.TeachingSelectionResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教授课课控制器: 实现用例 7.3 Select Courses to Teach。
 * <p>
 * 端点:
 * - GET    /teaching/offerings?semester=   获取有资格授课列表 + 已选标记
 * - POST   /teaching/select                 选择/取消授课课程(含时间冲突检测)
 * <p>
 * 安全: 强制 PROFESSOR 角色。
 */
@RestController
@RequestMapping("/teaching")
@RequiredArgsConstructor
public class TeachingController {

    private final TeachingService teachingService;
    private final RoleGuard roleGuard;

    private static final String CURRENT_SEMESTER = "2026-FALL";

    /**
     * 获取教授有资格授课的开设列表。
     * 备选流 No Course Offerings Available: 返回空列表(前端提示)。
     */
    @GetMapping("/offerings")
    public ResponseEntity<List<TeachingOfferingDTO>> getEligibleOfferings(
            @RequestParam(required = false) String semester) {
        Long profId = roleGuard.requireProfessor();
        String sem = semester != null ? semester : CURRENT_SEMESTER;
        return ResponseEntity.ok(teachingService.getEligibleOfferings(profId, sem));
    }

    /**
     * 选择/取消授课课程。
     * <p>
     * 备选流 Schedule Conflict: 返回 conflicts 详情,教授可调整后重试。
     * 备选流 Course Registration Closed: 返回 409。
     */
    @PostMapping("/select")
    public ResponseEntity<TeachingSelectionResult> selectCoursesToTeach(
            @RequestParam(required = false) String semester,
            @RequestBody SelectTeachingRequest request) {
        Long profId = roleGuard.requireProfessor();
        String sem = semester != null ? semester : CURRENT_SEMESTER;
        return ResponseEntity.ok(
                teachingService.selectCoursesToTeach(profId, sem, request.getOfferingIds()));
    }
}
