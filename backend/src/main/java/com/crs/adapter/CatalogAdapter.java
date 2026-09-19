package com.crs.adapter;

import com.crs.domain.Course;
import com.crs.domain.CourseOffering;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 课程目录适配器接口(只读)。
 * <p>
 * 对应需求第 9.2 节: 对遗留 Ingres Catalog 做只读适配器 + 中间层缓存,
 * 满足 ≤10s 延迟约束;写操作一律拒绝。
 * <p>
 * 实现类负责从不同数据源(遗留 Ingres / 缓存)获取课程和开设信息。
 */
public interface CatalogAdapter {

    /**
     * 获取单个课程信息。
     *
     * @param courseId 课程编号(如 "CS101")
     * @return 课程信息,不存在时返回 Optional.empty()
     */
    Optional<Course> getCourse(String courseId);

    /**
     * 获取指定学期的可用课程开设列表。
     *
     * @param semester 学期(如 "2026-FALL")
     * @return 该学期所有 OPEN 状态的课程开设
     */
    List<CourseOffering> getAvailableOfferings(String semester);

    /**
     * 批量获取课程信息。
     *
     * @param courseIds 课程编号列表
     * @return 课程编号到课程信息的映射,未找到的课程不包含在结果中
     */
    Map<String, Course> getCourses(List<String> courseIds);
}
