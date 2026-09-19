package com.crs.service;

import com.crs.domain.AuditLog;
import com.crs.repository.AuditLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 审计日志服务: 记录敏感操作的审计日志。
 * <p>
 * 对应需求第 10 节安全与审计要求:
 * - 登录成功/失败
 * - 成绩录入/修改
 * - 课表提交/删除
 * - 用户创建/修改/删除
 * - 查看成绩单
 * - 关闭注册
 */
@Slf4j
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * 记录审计日志。
     *
     * @param actorId   操作者 ID(可能为 null,如匿名操作)
     * @param action    操作类型
     * @param targetType 目标类型(如 User、Schedule、CourseOffering 等)
     * @param targetId  目标 ID
     * @param details   详细信息
     */
    @Transactional
    public void record(Long actorId, String action, String targetType,
                       Long targetId, String details) {
        AuditLog auditLog = new AuditLog();
        auditLog.setActorId(actorId);
        auditLog.setAction(action);
        auditLog.setTargetType(targetType);
        auditLog.setTargetId(targetId);
        auditLog.setDetails(details);
        auditLogRepository.save(auditLog);
        log.debug("审计日志: actor={}, action={}, target={}/{}", actorId, action, targetType, targetId);
    }

    // ==================== 便捷方法 ====================

    /** 记录登录成功 */
    public void recordLoginSuccess(Long userId, String username) {
        record(userId, "LOGIN_SUCCESS", "User", userId, "username=" + username);
    }

    /** 记录用户注册 */
    public void recordRegister(Long userId, String username) {
        record(userId, "REGISTER", "User", userId, "username=" + username);
    }

    /** 记录登录失败 */
    public void recordLoginFailed(String username, String reason) {
        record(null, "LOGIN_FAILED", "User", null,
                "username=" + username + ", reason=" + reason);
    }

    /** 记录成绩录入 */
    public void recordGradeEntry(Long professorId, Long offeringId, Long studentId,
                                 String courseId, String grade) {
        record(professorId, "GRADE_ENTRY", "Grade", null,
                String.format("professor=%d, offering=%d, student=%d, course=%s, grade=%s",
                        professorId, offeringId, studentId, courseId, grade));
    }

    /** 记录课表提交 */
    public void recordScheduleSubmit(Long studentId, Long scheduleId, String semester,
                                     boolean success, String details) {
        record(studentId, "SCHEDULE_SUBMIT", "Schedule", scheduleId,
                String.format("semester=%s, success=%s, %s", semester, success, details));
    }

    /** 记录课表删除 */
    public void recordScheduleDelete(Long studentId, Long scheduleId, String semester) {
        record(studentId, "SCHEDULE_DELETE", "Schedule", scheduleId,
                "semester=" + semester);
    }

    /** 记录用户创建 */
    public void recordUserCreate(Long registrarId, Long userId, String username, String role) {
        record(registrarId, "USER_CREATE", "User", userId,
                String.format("username=%s, role=%s", username, role));
    }

    /** 记录用户更新 */
    public void recordUserUpdate(Long registrarId, Long userId, String username) {
        record(registrarId, "USER_UPDATE", "User", userId,
                "username=" + username);
    }

    /** 记录用户删除 */
    public void recordUserDelete(Long registrarId, Long userId, String username) {
        record(registrarId, "USER_DELETE", "User", userId,
                "username=" + username);
    }

    /** 记录查看成绩单 */
    public void recordReportCardView(Long studentId, String semester, int courseCount) {
        record(studentId, "REPORT_CARD_VIEW", "Student", studentId,
                String.format("semester=%s, courses=%d", semester, courseCount));
    }

    /** 记录关闭注册 */
    public void recordRegistrationClose(Long registrarId, String semester,
                                         int totalOfferings, int committed, int cancelled) {
        record(registrarId, "REGISTRATION_CLOSE", "Registration", null,
                String.format("semester=%s, total=%d, committed=%d, cancelled=%d",
                        semester, totalOfferings, committed, cancelled));
    }

    /** 记录教授选择授课 */
    public void recordTeachingSelection(Long professorId, String semester,
                                         int selectedCount, boolean success) {
        record(professorId, "TEACHING_SELECT", "CourseOffering", null,
                String.format("semester=%s, count=%d, success=%s",
                        semester, selectedCount, success));
    }
}
