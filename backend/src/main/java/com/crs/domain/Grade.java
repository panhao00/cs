package com.crs.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 成绩(A / B / C / D / F / I,或 NULL 表示未录入)。
 * 对应需求: 成绩为敏感信息,需额外安全防护。
 */
@Entity
@Table(
    name = "grade",
    uniqueConstraints = @UniqueConstraint(columnNames = {"offering_id", "student_id"})
)
@Getter
@Setter
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "offering_id", nullable = false)
    private Long offeringId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /** A / B / C / D / F / I,或 null(未录入) */
    @Column(length = 2)
    private String grade;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
