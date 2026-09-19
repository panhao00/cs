package com.crs.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * 通知实体: 用于向学生发送课程取消、选课结果等通知。
 * <p>
 * 对应需求第 7.8 节备选流: 课程取消时通知学生。
 */
@Entity
@Table(name = "notification")
@Getter
@Setter
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "offering_id")
    private Long offeringId;

    @Column(name = "course_id", length = 20)
    private String courseId;

    @Column(nullable = false, name = "is_read")
    private boolean read = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 通知类型。
     */
    public enum NotificationType {
        /** 课程取消(人数不足或无教授) */
        OFFERING_CANCELLED,
        /** 选课成功 */
        ENROLLMENT_SUCCESS,
        /** 选课失败 */
        ENROLLMENT_FAILED,
        /** 注册关闭 */
        REGISTRATION_CLOSED,
        /** 成绩录入 */
        GRADE_POSTED,
        /** 容量警告(课程已满或接近满员) */
        CAPACITY_WARNING,
        /** 系统通知 */
        SYSTEM
    }
}
