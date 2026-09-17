package com.crs.web;

import com.crs.domain.Notification;
import com.crs.service.NotificationService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通知控制器: 管理学生通知。
 * <p>
 * 端点:
 * - GET  /notifications          获取当前学生通知列表
 * - GET  /notifications/unread   获取未读通知数量
 * - POST /notifications/{id}/read  标记通知为已读
 * - POST /notifications/read-all   标记所有通知为已读
 * <p>
 * 安全: 强制 STUDENT 角色;只能查看自己的通知。
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final RoleGuard roleGuard;

    /**
     * 获取当前学生通知列表。
     */
    @GetMapping
    public ResponseEntity<List<Notification>> getNotifications() {
        Long studentId = roleGuard.requireStudent();
        return ResponseEntity.ok(notificationService.getNotifications(studentId));
    }

    /**
     * 获取未读通知数量。
     */
    @GetMapping("/unread")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        Long studentId = roleGuard.requireStudent();
        long count = notificationService.getUnreadCount(studentId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * 标记通知为已读。
     */
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        Long studentId = roleGuard.requireStudent();
        notificationService.markAsRead(id, studentId);
        return ResponseEntity.ok().build();
    }

    /**
     * 标记所有通知为已读。
     */
    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        Long studentId = roleGuard.requireStudent();
        notificationService.markAllAsRead(studentId);
        return ResponseEntity.ok().build();
    }
}
