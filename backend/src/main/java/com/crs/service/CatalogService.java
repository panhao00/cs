package com.crs.service;

import com.crs.domain.Course;
import com.crs.domain.CourseOffering;
import com.crs.domain.EnrollmentStatus;
import com.crs.domain.OfferingStatus;
import com.crs.domain.User;
import com.crs.repository.CourseOfferingRepository;
import com.crs.repository.CourseRepository;
import com.crs.repository.EnrollmentRepository;
import com.crs.repository.UserRepository;
import com.crs.web.dto.CourseOfferingDTO;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 课程目录服务(只读适配器 + 中间层缓存)。
 * <p>
 * 对应需求第 9.2 节: 对遗留 Catalog 做只读适配器 + 中间层缓存,
 * 满足 ≤10s 延迟约束;写操作一律拒绝。
 * <p>
 * 当前实现: 使用本地表模拟 Catalog,并加请求级 + 短期 TTL 缓存。
 * 后续可替换为真实 Ingres 访问层,接口保持不变。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogService {

    private final CourseRepository courseRepository;
    private final CourseOfferingRepository offeringRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    /** 简易缓存: semester -> (offeringId -> DTO),TTL 30s。模拟中间层缓存。 */
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private static final long CACHE_TTL_MS = 30_000;

    /**
     * 查询某学期可用的课程开设列表(学生选课用)。
     */
    @Transactional(readOnly = true)
    public List<CourseOfferingDTO> getAvailableOfferings(String semester) {
        CacheEntry entry = cache.get(semester);
        long now = System.currentTimeMillis();
        if (entry != null && now - entry.timestamp < CACHE_TTL_MS) {
            log.debug("命中缓存: semester={}", semester);
            return entry.dtos;
        }

        log.debug("未命中缓存,查询数据库: semester={}", semester);
        List<CourseOffering> offerings = offeringRepository
                .findBySemesterAndStatus(semester, OfferingStatus.OPEN);

        List<CourseOfferingDTO> dtos = offerings.stream()
                .map(this::toDTO)
                .toList();

        cache.put(semester, new CacheEntry(dtos, now));
        return dtos;
    }

    /**
     * 获取单个开设详情(不缓存,确保容量实时)。
     */
    @Transactional(readOnly = true)
    public Optional<CourseOfferingDTO> getOffering(Long offeringId) {
        return offeringRepository.findById(offeringId).map(this::toDTO);
    }

    /** 课程目录只读: 查找课程 */
    @Transactional(readOnly = true)
    public Optional<Course> getCourse(String courseId) {
        return courseRepository.findById(courseId);
    }

    /** 批量查找课程 */
    @Transactional(readOnly = true)
    public Map<String, Course> getCourses(List<String> courseIds) {
        Map<String, Course> result = new HashMap<>();
        for (String id : courseIds) {
            courseRepository.findById(id).ifPresent(c -> result.put(id, c));
        }
        return result;
    }

    // ---- 内部方法 ----

    private CourseOfferingDTO toDTO(CourseOffering o) {
        long enrolled = enrollmentRepository.countByOfferingIdAndStatus(
                o.getId(), EnrollmentStatus.ENROLLED);
        Course course = courseRepository.findById(o.getCourseId()).orElse(null);
        String profName = null;
        if (o.getProfessorId() != null) {
            profName = userRepository.findById(o.getProfessorId())
                    .map(User::getName).orElse(null);
        }
        int maxCap = o.getMaxCapacity() != null ? o.getMaxCapacity() : 10;
        return new CourseOfferingDTO(
                o.getId(),
                o.getCourseId(),
                course != null ? course.getTitle() : null,
                course != null ? course.getDepartment() : null,
                course != null ? course.getCredits() : null,
                course != null ? course.getPrerequisiteId() : null,
                o.getSemester(),
                o.getDays(),
                o.getTimes(),
                o.getRoom(),
                maxCap,
                (int) enrolled,
                maxCap - (int) enrolled,
                o.getStatus(),
                o.getProfessorId(),
                profName
        );
    }

    private record CacheEntry(List<CourseOfferingDTO> dtos, long timestamp) {}
}
