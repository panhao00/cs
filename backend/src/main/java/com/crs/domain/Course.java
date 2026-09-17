package com.crs.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 课程(来自遗留课程目录,只读)。
 * 对应需求: 新系统只读访问 Course Catalog,不修改。
 */
@Entity
@Table(name = "course")
@Getter
@Setter
public class Course {

    @Id
    @Column(name = "course_id", length = 20)
    private String courseId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 50)
    private String department;

    private Integer credits = 3;

    /** 先修课(自引用) */
    @Column(name = "prerequisite_id", length = 20)
    private String prerequisiteId;
}
