package com.crs.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * 课程评价(学生对某开设的评分+评论)。
 * 业务规则: 一个学生对一个开设只能评价一次(offeringId+studentId 唯一约束)。
 */
@Entity
@Table(
    name = "review",
    uniqueConstraints = @UniqueConstraint(columnNames = {"offering_id", "student_id"})
)
@Getter
@Setter
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "offering_id", nullable = false)
    private Long offeringId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /** 评分 1-5 */
    @Column(nullable = false)
    private Integer rating;

    /** 评论内容,最长 500 字 */
    @Column(length = 500)
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
