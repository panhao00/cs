package com.crs.repository;

import com.crs.domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 课程目录仓储(只读: 数据来自遗留 Ingres Catalog)。
 */
@Repository
public interface CourseRepository extends JpaRepository<Course, String> {
}
