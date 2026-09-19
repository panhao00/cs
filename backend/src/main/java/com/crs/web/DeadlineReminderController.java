package com.crs.web;

import com.crs.service.DeadlineReminderService;
import com.crs.web.dto.RegistrationDeadline;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 选课倒计时提醒控制器。
 * <p>
 * 端点:
 * - GET /deadlines?semester=  获取时间节点列表(需登录)
 */
@RestController
@RequestMapping("/deadlines")
@RequiredArgsConstructor
public class DeadlineReminderController {

    private final DeadlineReminderService deadlineReminderService;
    private final RoleGuard roleGuard;

    /**
     * 获取当前学期的重要时间节点列表。
     *
     * @param semester 学期,如 "2026-FALL"
     */
    @GetMapping
    public ResponseEntity<List<RegistrationDeadline>> getDeadlines(
            @RequestParam String semester) {
        roleGuard.requireAnyLogin();
        return ResponseEntity.ok(deadlineReminderService.getUpcomingDeadlines(semester));
    }
}
