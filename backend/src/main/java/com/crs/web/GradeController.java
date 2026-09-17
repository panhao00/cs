package com.crs.web;

import com.crs.service.GradeService;
import com.crs.web.dto.GradeEntryRequest;
import com.crs.web.dto.StudentGradeDTO;
import com.crs.web.dto.TeachingOfferingDTO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * 成绩控制器: 实现用例 7.4 Submit Grades。
 * <p>
 * 端点:
 * - GET   /grades/taught                  上学期授课列表
 * - GET   /grades/offerings/{id}/roster   某班级名单 + 成绩
 * - POST  /grades/offerings/{id}          录入/修改成绩
 * <p>
 * 安全: 强制 PROFESSOR 角色;只能操作自己任教的开设。
 */
@RestController
@RequestMapping("/grades")
@RequiredArgsConstructor
public class GradeController {

    private final GradeService gradeService;
    private final RoleGuard roleGuard;

    /**
     * 获取教授上学期授课列表(基本流 1)。
     * 备选流 No Course Offerings Taught: 返回空列表。
     */
    @GetMapping("/taught")
    public ResponseEntity<List<TeachingOfferingDTO>> getTaughtOfferings() {
        Long profId = roleGuard.requireProfessor();
        List<TeachingOfferingDTO> offerings = gradeService.getTaughtOfferings(profId);
        return ResponseEntity.ok(offerings);
    }

    /**
     * 获取某班级名单 + 成绩(基本流 3)。
     */
    @GetMapping("/offerings/{offeringId}/roster")
    public ResponseEntity<List<StudentGradeDTO>> getRoster(
            @PathVariable Long offeringId) {
        Long profId = roleGuard.requireProfessor();
        return ResponseEntity.ok(gradeService.getRosterWithGrades(profId, offeringId));
    }

    /**
     * 录入/修改成绩(基本流 4)。
     * <p>
     * 成绩取值 A/B/C/D/F/I;可修改已录成绩;clear=true 清除成绩(留空补录)。
     */
    @PostMapping("/offerings/{offeringId}")
    public ResponseEntity<StudentGradeDTO> submitGrade(
            @PathVariable Long offeringId,
            @Valid @RequestBody GradeEntryRequest request) {
        Long profId = roleGuard.requireProfessor();
        return ResponseEntity.ok(gradeService.submitGrade(
                profId, offeringId, request.getStudentId(),
                request.getGrade(), request.isClear()));
    }
}
