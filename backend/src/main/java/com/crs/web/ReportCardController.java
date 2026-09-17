package com.crs.web;

import com.crs.service.ReportCardService;
import com.crs.web.dto.ReportCardDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 成绩单控制器: 实现用例 7.5 View Report Card。
 * <p>
 * 端点:
 * - GET /report-card  查看当前学生上学期成绩单
 * <p>
 * 安全: 强制 STUDENT 角色;只能查看自己的成绩单(水平越权防护);
 * 查看操作记录审计日志(在 Service 中)。
 */
@RestController
@RequestMapping("/report-card")
@RequiredArgsConstructor
public class ReportCardController {

    private final ReportCardService reportCardService;
    private final RoleGuard roleGuard;

    /**
     * 查看当前学生上学期成绩单。
     * <p>
     * 备选流 No Grades Available: 返回空成绩单,前端可提示。
     */
    @GetMapping
    public ResponseEntity<ReportCardDTO> getMyReportCard() {
        Long studentId = roleGuard.requireStudent();
        return ResponseEntity.ok(reportCardService.getReportCard(studentId));
    }
}
