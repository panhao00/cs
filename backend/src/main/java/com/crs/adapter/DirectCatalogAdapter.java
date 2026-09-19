package com.crs.adapter;

import com.crs.domain.Course;
import com.crs.domain.CourseOffering;
import com.crs.domain.OfferingStatus;
import com.crs.repository.CourseOfferingRepository;
import com.crs.repository.CourseRepository;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 直连适配器: 通过 JPA Repository 访问课程目录(非遗留模式)。
 * <p>
 * 当 {@code crs.catalog.legacy.enabled=false} 时使用此适配器,
 * 保持与改造前完全一致的行为(直接访问本地 JPA 实体)。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DirectCatalogAdapter implements CatalogAdapter {

    private final CourseRepository courseRepository;
    private final CourseOfferingRepository offeringRepository;

    @Override
    public Optional<Course> getCourse(String courseId) {
        log.debug("DirectCatalogAdapter: 查询课程 courseId={}", courseId);
        return courseRepository.findById(courseId);
    }

    @Override
    public List<CourseOffering> getAvailableOfferings(String semester) {
        log.debug("DirectCatalogAdapter: 查询学期开设 semester={}", semester);
        return offeringRepository.findBySemesterAndStatus(semester, OfferingStatus.OPEN);
    }

    @Override
    public Map<String, Course> getCourses(List<String> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        log.debug("DirectCatalogAdapter: 批量查询课程 courseIds={}", courseIds);
        Map<String, Course> result = new HashMap<>();
        for (String id : courseIds) {
            courseRepository.findById(id).ifPresent(c -> result.put(id, c));
        }
        return result;
    }
}
