package com.crs.web;

import com.crs.service.AcademicSummaryService;
import com.crs.service.ScheduleService;
import com.crs.web.dto.AcademicSummaryDTO;
import com.crs.web.dto.ScheduleResponse;
import com.crs.web.dto.SelectOfferingsRequest;
import com.crs.web.dto.SubmitResult;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 课表控制器: 实现用例 7.2 Register for Courses。
 * <p>
 * 端点:
 * - GET    /schedules/me?semester=    获取当前学生课表(No Schedule Found 备选流)
 * - POST   /schedules                  创建课表(Create a Schedule)
 * - POST   /schedules/offerings        选择开设(Select Offerings)
 * - DELETE /schedules/offerings/{id}   从课表移除开设
 * - POST   /schedules/save             保存课表(Save a Schedule 备选流)
 * - POST   /schedules/submit           提交课表(Submit Schedule,含校验)
 * - DELETE /schedules                   删除课表(Delete a Schedule)
 * - GET    /schedules/academic-summary   获取学生学业摘要(GPA/学分/课程历史)
 * <p>
 * 安全: 所有端点强制 STUDENT 角色,且只能操作自己的课表(水平越权防护)。
 */
@Slf4j
@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final AcademicSummaryService academicSummaryService;
    private final RoleGuard roleGuard;

    /** 当前学期(简化: 配置项,实际可从注册周期表查询) */
    private static final String CURRENT_SEMESTER = "2026-FALL";

    /**
     * 获取当前学生课表。
     * 若不存在返回 404(对应 No Schedule Found 备选流)。
     */
    @GetMapping("/me")
    public ResponseEntity<ScheduleResponse> getMySchedule(
            @RequestParam(required = false) String semester) {
        Long userId = roleGuard.requireStudent();
        String sem = semester != null ? semester : CURRENT_SEMESTER;
        return ResponseEntity.of(scheduleService.getMySchedule(userId, sem));
    }

    /**
     * 创建课表(Create a Schedule 基本流)。
     */
    @PostMapping
    public ResponseEntity<ScheduleResponse> createSchedule(
            @RequestParam(required = false) String semester) {
        Long userId = roleGuard.requireStudent();
        String sem = semester != null ? semester : CURRENT_SEMESTER;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(scheduleService.createSchedule(userId, sem));
    }

    /**
     * 选择课程开设(Select Offerings 子流)。
     */
    @PostMapping("/offerings")
    public ResponseEntity<ScheduleResponse> selectOfferings(
            @RequestParam(required = false) String semester,
            @Valid @RequestBody SelectOfferingsRequest request) {
        Long userId = roleGuard.requireStudent();
        String sem = semester != null ? semester : CURRENT_SEMESTER;
        return ResponseEntity.ok(
                scheduleService.selectOfferings(userId, sem, request));
    }

    /**
     * 从课表移除某开设(Update a Schedule 中的"删"动作)。
     */
    @DeleteMapping("/offerings/{offeringId}")
    public ResponseEntity<ScheduleResponse> removeOffering(
            @PathVariable Long offeringId,
            @RequestParam(required = false) String semester) {
        Long userId = roleGuard.requireStudent();
        String sem = semester != null ? semester : CURRENT_SEMESTER;
        return ResponseEntity.ok(
                scheduleService.removeOffering(userId, sem, offeringId));
    }

    /**
     * 保存课表不提交(Save a Schedule 备选流)。
     */
    @PostMapping("/save")
    public ResponseEntity<ScheduleResponse> saveSchedule(
            @RequestParam(required = false) String semester) {
        Long userId = roleGuard.requireStudent();
        String sem = semester != null ? semester : CURRENT_SEMESTER;
        return ResponseEntity.ok(scheduleService.saveSchedule(userId, sem));
    }

    /**
     * 提交课表(Submit Schedule 子流,含校验)。
     * <p>
     * 校验失败的开设在 errors 中列出,学生可据此调整后重试。
     * 满员通知: CAPACITY_FULL 类型错误在学生提交前告知。
     */
    @PostMapping("/submit")
    public ResponseEntity<SubmitResult> submitSchedule(
            @RequestParam(required = false) String semester) {
        Long userId = roleGuard.requireStudent();
        String sem = semester != null ? semester : CURRENT_SEMESTER;
        SubmitResult result = scheduleService.submitSchedule(userId, sem);
        return ResponseEntity.ok(result);
    }

    /**
     * 删除课表(Delete a Schedule 基本流)。
     */
    @DeleteMapping
    public ResponseEntity<Map<String, String>> deleteSchedule(
            @RequestParam(required = false) String semester) {
        Long userId = roleGuard.requireStudent();
        String sem = semester != null ? semester : CURRENT_SEMESTER;
        scheduleService.deleteSchedule(userId, sem);
        return ResponseEntity.ok(Map.of("message", "课表已删除"));
    }

    /**
     * 获取学生学业摘要: 累计 GPA、学分统计和按学期分组的课程历史。
     * <p>
     * 返回内容:
     * - cumulativeGpa: 累计 GPA(所有有成绩学期的加权平均)
     * - totalCreditsEarned: 已获得学分(成绩 A/B/C/D)
     * - totalCreditsAttempted: 已修读学分(含 F/I)
     * - currentEnrolledCredits: 当前学期选课学分
     * - semesterHistory: 按学期分组的课程历史(逆序,最新学期在前)
     */
    @GetMapping("/academic-summary")
    public ResponseEntity<AcademicSummaryDTO> getAcademicSummary() {
        Long studentId = roleGuard.requireStudent();
        return ResponseEntity.ok(academicSummaryService.getSummary(studentId));
    }

}
