package com.crs.domain;

import jakarta.persistence.*;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 课程开设(某学期某课程的具体班级)。
 * 业务规则: 最多 10 人,最少 3 人,不足则取消。
 */
@Entity
@Table(name = "course_offering")
@Getter
@Setter
public class CourseOffering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "offering_id")
    private Long id;

    @Column(name = "course_id", nullable = false, length = 20)
    private String courseId;

    @Column(nullable = false, length = 20)
    private String semester;

    /** 教授 ID;为 null 表示尚未指派教授 */
    @Column(name = "professor_id")
    private Long professorId;

    /** 上课日,如 "MWF" / "TR" */
    @Column(length = 20)
    private String days;

    /** 上课时间,如 "09:00-10:00" */
    @Column(length = 50)
    private String times;

    @Column(length = 50)
    private String room;

    @Column(name = "max_capacity")
    private Integer maxCapacity = 10;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private OfferingStatus status = OfferingStatus.OPEN;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;
}
