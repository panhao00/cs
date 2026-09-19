package com.crs.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * 候补名单记录(学生在课程满员时排队等待递补)。
 * <p>
 * 业务规则:
 * - 课程已满时学生可加入候补,按 position 排队
 * - 当有人退选时,按 position 顺序递补下一位学生
 * - 递补时检查: 课程仍在 OPEN 状态, 仍有容量, 学生满足先修课要求
 */
@Entity
@Table(
    name = "waitlist",
    uniqueConstraints = @UniqueConstraint(columnNames = {"offering_id", "student_id"})
)
@Getter
@Setter
public class Waitlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "offering_id", nullable = false)
    private Long offeringId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /** 排队位置(从 1 开始,按加入顺序递增) */
    @Column(nullable = false)
    private Integer position;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private WaitlistStatus status = WaitlistStatus.WAITING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
