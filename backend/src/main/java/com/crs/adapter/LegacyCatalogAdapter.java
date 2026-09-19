package com.crs.adapter;

import com.crs.domain.Course;
import com.crs.domain.CourseOffering;
import com.crs.domain.OfferingStatus;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

/**
 * 遗留 Ingres 课程目录适配器(只读)。
 * <p>
 * 通过 JdbcTemplate 模拟对遗留 Ingres 数据库的 SQL 查询。
 * <p>
 * <b>注意:</b> 当前实现使用本地 MySQL 表模拟遗留 Ingres 数据。
 * 生产部署时需将 SQL 方言替换为 Ingres SQL/CE,并配置 Ingres JDBC 数据源。
 * <p>
 * 对应需求第 9.2 节: 对遗留 Catalog 做只读适配器 + 中间层缓存,
 * 满足 ≤10s 延迟约束。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LegacyCatalogAdapter implements CatalogAdapter {

    private final JdbcTemplate jdbcTemplate;

    // ---- Row Mappers ----

    private final RowMapper<Course> courseRowMapper = (rs, rowNum) -> {
        Course course = new Course();
        course.setCourseId(rs.getString("course_id"));
        course.setTitle(rs.getString("title"));
        course.setDepartment(rs.getString("department"));
        course.setCredits(rs.getInt("credits"));
        course.setPrerequisiteId(rs.getString("prerequisite_id"));
        return course;
    };

    private final RowMapper<CourseOffering> offeringRowMapper = (rs, rowNum) -> {
        CourseOffering offering = new CourseOffering();
        offering.setId(rs.getLong("offering_id"));
        offering.setCourseId(rs.getString("course_id"));
        offering.setSemester(rs.getString("semester"));
        offering.setProfessorId(rs.getObject("professor_id", Long.class));
        offering.setDays(rs.getString("days"));
        offering.setTimes(rs.getString("times"));
        offering.setRoom(rs.getString("room"));
        offering.setMaxCapacity(rs.getInt("max_capacity"));
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            offering.setStatus(OfferingStatus.valueOf(statusStr));
        }
        return offering;
    };

    // ---- CatalogAdapter Implementation ----

    /**
     * 从遗留 Ingres 目录获取单个课程信息。
     * <p>
     * <b>模拟 SQL (Ingres 方言):</b>
     * <pre>
     * SELECT course_id, title, department, credits, prerequisite_id
     * FROM   ingres_catalog.course
     * WHERE  course_id = ?
     * </pre>
     */
    @Override
    public Optional<Course> getCourse(String courseId) {
        log.debug("LegacyCatalogAdapter: 查询课程 courseId={}", courseId);

        // 模拟 SQL: SELECT course_id, title, department, credits, prerequisite_id
        //           FROM   course WHERE course_id = ?
        // 实际应替换为 Ingres JDBC 调用:
        //   ingressJdbcTemplate.queryForObject(sql, courseRowMapper, courseId);
        String sql = "SELECT course_id, title, department, credits, prerequisite_id "
                + "FROM course WHERE course_id = ?";

        try {
            Course course = jdbcTemplate.queryForObject(sql, courseRowMapper, courseId);
            return Optional.ofNullable(course);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 从遗留 Ingres 目录获取指定学期的可用课程开设列表。
     * <p>
     * <b>模拟 SQL (Ingres 方言):</b>
     * <pre>
     * SELECT offering_id, course_id, semester, professor_id,
     *        days, times, room, max_capacity, status
     * FROM   ingres_catalog.course_offering
     * WHERE  semester = ? AND status = 'OPEN'
     * ORDER BY course_id
     * </pre>
     */
    @Override
    public List<CourseOffering> getAvailableOfferings(String semester) {
        log.debug("LegacyCatalogAdapter: 查询学期开设 semester={}", semester);

        // 模拟 SQL: SELECT offering_id, course_id, semester, professor_id,
        //           days, times, room, max_capacity, status
        //           FROM course_offering WHERE semester = ? AND status = 'OPEN'
        // 实际应替换为 Ingres JDBC 调用:
        //   ingressJdbcTemplate.query(sql, offeringRowMapper, semester);
        String sql = "SELECT offering_id, course_id, semester, professor_id, "
                + "days, times, room, max_capacity, status "
                + "FROM course_offering "
                + "WHERE semester = ? AND status = 'OPEN' "
                + "ORDER BY course_id";

        return jdbcTemplate.query(sql, offeringRowMapper, semester);
    }

    /**
     * 从遗留 Ingres 目录批量获取课程信息。
     * <p>
     * <b>模拟 SQL (Ingres 方言):</b>
     * <pre>
     * SELECT course_id, title, department, credits, prerequisite_id
     * FROM   ingres_catalog.course
     * WHERE  course_id IN (?, ?, ...)
     * </pre>
     */
    @Override
    public Map<String, Course> getCourses(List<String> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return Collections.emptyMap();
        }

        log.debug("LegacyCatalogAdapter: 批量查询课程 courseIds={}", courseIds);

        // 模拟 SQL: SELECT course_id, title, department, credits, prerequisite_id
        //           FROM course WHERE course_id IN (?, ?, ...)
        // 实际应替换为 Ingres JDBC 调用:
        //   ingressJdbcTemplate.query(sql, courseRowMapper, courseIds.toArray());
        String placeholders = String.join(",", Collections.nCopies(courseIds.size(), "?"));
        String sql = "SELECT course_id, title, department, credits, prerequisite_id "
                + "FROM course WHERE course_id IN (" + placeholders + ")";

        List<Course> courses = jdbcTemplate.query(sql, courseRowMapper, courseIds.toArray());

        Map<String, Course> result = new HashMap<>();
        for (Course course : courses) {
            result.put(course.getCourseId(), course);
        }
        return result;
    }
}
