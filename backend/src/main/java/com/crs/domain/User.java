package com.crs.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 用户实体(统一存储 Student / Professor / Registrar 三类角色)。
 * 对应 schema.sql 中的 app_user 表。
 * <p>
 * 设计决策: 采用单表策略而非 JOINED 继承——三类角色字段重叠度高,
 * 且 Login 用例仅需 username/password/role,单表可简化查询。
 * 后续如字段差异增大可重构为继承。
 */
@Entity
@Table(name = "app_user")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(length = 20)
    private String ssn;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    // ---- 仅学生使用 ----
    @Column(name = "graduation_date")
    private LocalDate graduationDate;

    // ---- 仅教授使用 ----
    @Column(length = 50)
    private String department;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 用户状态枚举 */
    public enum UserStatus {
        ACTIVE,
        INACTIVE
    }
}
