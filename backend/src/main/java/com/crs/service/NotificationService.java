package com.crs.service;

import com.crs.domain.CourseOffering;
import com.crs.domain.Enrollment;
import com.crs.domain.EnrollmentStatus;
import com.crs.domain.Notification;
import com.crs.domain.Notification.NotificationType;
import com.crs.repository.CourseOfferingRepository;
import com.crs.repository.EnrollmentRepository;
import com.crs.repository.NotificationRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 通知服务: 管理学生通知。
 * <p>
 * 主要用途:
 * - 课程取消时通知受影响的学生
 * - 注册关闭时通知所有学生
 * - 选课成功/失败时通知学生
 * <p>
 * 参考 AI 生成,经人工核验与调整。
 */
@Slf4j
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseOfferingRepository offeringRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            EnrollmentRepository enrollmentRepository,
            CourseOfferingRepository offeringRepository) {
        this.notificationRepository = notificationRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.offeringRepository = offeringRepository;
    }

    /**
     * 获取学生的所有通知(按时间倒序)。
     */
    @Transactional(readOnly = true)
    public List<Notification> getNotifications(Long studentId) {
        return notificationRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
    }

    /**
     * 获取学生未读通知数量。
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(Long studentId) {
        return notificationRepository.countByStudentIdAndReadFalse(studentId);
    }

    /**
     * 标记通知为已读。
     */
    @Transactional
    public void markAsRead(Long notificationId, Long studentId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElse(null);
        if (notification != null && notification.getStudentId().equals(studentId)) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    /**
     * 标记学生所有通知为已读。
     */
    @Transactional
    public void markAllAsRead(Long studentId) {
        List<Notification> unread = notificationRepository
                .findByStudentIdAndReadFalseOrderByCreatedAtDesc(studentId);
        for (Notification n : unread) {
            n.setRead(true);
            notificationRepository.save(n);
        }
        log.info("学生 {} 的 {} 条通知已标记为已读", studentId, unread.size());
    }

    /**
     * 通知学生课程已取消(7.8 备选流)。
     * <p>
     * 当课程开设因人数不足或无教授而被取消时,通知所有已选该课程的学生。
     */
    @Transactional
    public void notifyCourseCancelled(Long offeringId) {
        CourseOffering offering = offeringRepository.findById(offeringId).orElse(null);
        if (offering == null) return;

        List<Enrollment> enrollments = enrollmentRepository
                .findByOfferingIdAndStatus(offeringId, EnrollmentStatus.ENROLLED);

        for (Enrollment e : enrollments) {
            createNotification(
                    e.getStudentId(),
                    NotificationType.OFFERING_CANCELLED,
                    "课程取消通知",
                    String.format("您选择的课程 %s 因未达到最小人数要求已被取消。如有疑问请联系注册办公室。",
                            offering.getCourseId()),
                    offeringId,
                    offering.getCourseId()
            );

            // 同时取消该学生的选课记录
            e.setStatus(EnrollmentStatus.CANCELLED);
            enrollmentRepository.save(e);
        }

        log.info("已通知 {} 名学生: 课程 {} 已取消", enrollments.size(), offering.getCourseId());
    }

    /**
     * 通知学生注册已关闭。
     */
    @Transactional
    public void notifyRegistrationClosed(Long studentId, String semester) {
        createNotification(
                studentId,
                NotificationType.REGISTRATION_CLOSED,
                "注册已关闭",
                String.format("%s 学期的注册已关闭。您的最终课表已确认,请查看课程表。",
                        semester),
                null,
                null
        );
    }

    /**
     * 通知学生选课成功。
     */
    @Transactional
    public void notifyEnrollmentSuccess(Long studentId, Long offeringId, String courseId) {
        createNotification(
                studentId,
                NotificationType.ENROLLMENT_SUCCESS,
                "选课成功",
                String.format("您已成功注册课程 %s。", courseId),
                offeringId,
                courseId
        );
    }

    /**
     * 通知学生选课失败。
     */
    @Transactional
    public void notifyEnrollmentFailed(Long studentId, Long offeringId, String courseId,
                                       String reason) {
        createNotification(
                studentId,
                NotificationType.ENROLLMENT_FAILED,
                "选课失败",
                String.format("课程 %s 选课失败: %s", courseId, reason),
                offeringId,
                courseId
        );
    }

    /**
     * 通知学生成绩已录入。
     */
    @Transactional
    public void notifyGradePosted(Long studentId, Long offeringId, String courseId,
                                  String grade) {
        createNotification(
                studentId,
                NotificationType.GRADE_POSTED,
                "成绩已录入",
                String.format("课程 %s 的成绩已录入: %s", courseId, grade),
                offeringId,
                courseId
        );
    }

    // ==================== 内部方法 ====================

    private void createNotification(Long studentId, NotificationType type,
                                    String title, String content,
                                    Long offeringId, String courseId) {
        Notification notification = new Notification();
        notification.setStudentId(studentId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setOfferingId(offeringId);
        notification.setCourseId(courseId);
        notification.setRead(false);
        notificationRepository.save(notification);
        log.info("创建通知: studentId={}, type={}, title={}", studentId, type, title);
    }
}
