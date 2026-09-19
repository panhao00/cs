package com.crs.adapter;

import com.crs.domain.Course;
import com.crs.domain.CourseOffering;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

/**
 * 缓存装饰器: 两级缓存的课程目录适配器(只读)。
 * <p>
 * <b>缓存策略:</b>
 * <ul>
 *   <li>L1 — 内存缓存: ConcurrentHashMap + TTL 30 秒,快速命中</li>
 *   <li>L2 — 本地数据库物化视图模拟: 内存 Map 模拟 DB 物化视图,
 *        在 L1 TTL 过期后提供降级查询,避免直接穿透到遗留 Ingres</li>
 * </ul>
 * <p>
 * <b>查询流程:</b>
 * <ol>
 *   <li>先查 L1 内存缓存,命中则返回</li>
 *   <li>L1 未命中,查 L2 物化视图(如果有效)</li>
 *   <li>L2 也未命中,查询底层 LegacyCatalogAdapter 并更新两级缓存</li>
 * </ol>
 * <p>
 * 对应需求第 9.2 节: 中间层缓存,满足 ≤10s 延迟约束。
 *
 * @see LegacyCatalogAdapter
 */
@Slf4j
public class CachedCatalogAdapter implements CatalogAdapter {

    private final CatalogAdapter delegate;

    /** L1 缓存 TTL(毫秒) */
    private static final long L1_CACHE_TTL_MS = 30_000;

    /** L2 物化视图 TTL(毫秒),比 L1 长 5 倍 */
    private static final long L2_CACHE_TTL_MS = 150_000;

    // ---- L1 内存缓存 ----

    /** L1: courseId -> CacheEntry<Course> */
    private final ConcurrentHashMap<String, L1Entry<Course>> courseL1Cache = new ConcurrentHashMap<>();

    /** L1: semester -> CacheEntry<List<CourseOffering>> */
    private final ConcurrentHashMap<String, L1Entry<List<CourseOffering>>> offeringL1Cache = new ConcurrentHashMap<>();

    /** L1: 批量课程缓存,以排序后的 courseIds 拼接为 key */
    private final ConcurrentHashMap<String, L1Entry<Map<String, Course>>> coursesL1Cache = new ConcurrentHashMap<>();

    // ---- L2 物化视图模拟(本地数据库) ----

    /** L2: courseId -> Course(模拟物化视图中的课程快照) */
    private final ConcurrentHashMap<String, L2Entry<Course>> courseL2View = new ConcurrentHashMap<>();

    /** L2: semester -> List<CourseOffering>(模拟物化视图中的开设快照) */
    private final ConcurrentHashMap<String, L2Entry<List<CourseOffering>>> offeringL2View = new ConcurrentHashMap<>();

    /** L2: sorted courseIds key -> Map<String, Course>(模拟物化视图中的批量快照) */
    private final ConcurrentHashMap<String, L2Entry<Map<String, Course>>> coursesL2View = new ConcurrentHashMap<>();

    public CachedCatalogAdapter(CatalogAdapter delegate) {
        this.delegate = delegate;
    }

    // ---- CatalogAdapter Implementation ----

    @Override
    public Optional<Course> getCourse(String courseId) {
        long now = System.currentTimeMillis();

        // L1: 内存缓存
        L1Entry<Course> l1 = courseL1Cache.get(courseId);
        if (l1 != null && now - l1.timestamp < L1_CACHE_TTL_MS) {
            log.debug("CachedCatalogAdapter L1 命中: courseId={}", courseId);
            return Optional.ofNullable(l1.value);
        }

        // L2: 物化视图模拟
        L2Entry<Course> l2 = courseL2View.get(courseId);
        if (l2 != null && now - l2.timestamp < L2_CACHE_TTL_MS) {
            log.debug("CachedCatalogAdapter L2 命中: courseId={}", courseId);
            Course value = l2.value;
            // 回填 L1
            courseL1Cache.put(courseId, new L1Entry<>(value, now));
            return Optional.ofNullable(value);
        }

        // Cache miss: 查询底层适配器
        log.debug("CachedCatalogAdapter 缓存未命中,查询底层: courseId={}", courseId);
        Optional<Course> result = delegate.getCourse(courseId);
        Course value = result.orElse(null);
        courseL1Cache.put(courseId, new L1Entry<>(value, now));
        courseL2View.put(courseId, new L2Entry<>(value, now));
        return result;
    }

    @Override
    public List<CourseOffering> getAvailableOfferings(String semester) {
        long now = System.currentTimeMillis();

        // L1: 内存缓存
        L1Entry<List<CourseOffering>> l1 = offeringL1Cache.get(semester);
        if (l1 != null && now - l1.timestamp < L1_CACHE_TTL_MS) {
            log.debug("CachedCatalogAdapter L1 命中: semester={}", semester);
            return l1.value != null ? l1.value : Collections.emptyList();
        }

        // L2: 物化视图模拟
        L2Entry<List<CourseOffering>> l2 = offeringL2View.get(semester);
        if (l2 != null && now - l2.timestamp < L2_CACHE_TTL_MS) {
            log.debug("CachedCatalogAdapter L2 命中: semester={}", semester);
            List<CourseOffering> value = l2.value;
            // 回填 L1
            offeringL1Cache.put(semester, new L1Entry<>(value, now));
            return value != null ? value : Collections.emptyList();
        }

        // Cache miss: 查询底层适配器
        log.debug("CachedCatalogAdapter 缓存未命中,查询底层: semester={}", semester);
        List<CourseOffering> result = delegate.getAvailableOfferings(semester);
        offeringL1Cache.put(semester, new L1Entry<>(result, now));
        offeringL2View.put(semester, new L2Entry<>(result, now));
        return result;
    }

    @Override
    public Map<String, Course> getCourses(List<String> courseIds) {
        long now = System.currentTimeMillis();
        String cacheKey = buildCoursesCacheKey(courseIds);

        // L1: 内存缓存
        L1Entry<Map<String, Course>> l1 = coursesL1Cache.get(cacheKey);
        if (l1 != null && now - l1.timestamp < L1_CACHE_TTL_MS) {
            log.debug("CachedCatalogAdapter L1 命中: courses key={}", cacheKey);
            return l1.value != null ? l1.value : Collections.emptyMap();
        }

        // L2: 物化视图模拟
        L2Entry<Map<String, Course>> l2 = coursesL2View.get(cacheKey);
        if (l2 != null && now - l2.timestamp < L2_CACHE_TTL_MS) {
            log.debug("CachedCatalogAdapter L2 命中: courses key={}", cacheKey);
            Map<String, Course> value = l2.value;
            // 回填 L1
            coursesL1Cache.put(cacheKey, new L1Entry<>(value, now));
            return value != null ? value : Collections.emptyMap();
        }

        // Cache miss: 查询底层适配器
        log.debug("CachedCatalogAdapter 缓存未命中,查询底层: courses key={}", cacheKey);
        Map<String, Course> result = delegate.getCourses(courseIds);
        coursesL1Cache.put(cacheKey, new L1Entry<>(result, now));
        coursesL2View.put(cacheKey, new L2Entry<>(result, now));
        return result;
    }

    // ---- 手动刷新 ----

    /**
     * 手动刷新全部缓存(L1 + L2)。
     * <p>
     * 适用于数据源发生变更时的主动失效场景。
     */
    public void refreshAll() {
        log.info("刷新全部缓存: 清除 {} 条 L1 课程缓存, {} 条 L1 开设缓存, "
                + "{} 条 L2 物化视图",
                courseL1Cache.size(), offeringL1Cache.size(), courseL2View.size());
        courseL1Cache.clear();
        offeringL1Cache.clear();
        coursesL1Cache.clear();
        courseL2View.clear();
        offeringL2View.clear();
        coursesL2View.clear();
    }

    /**
     * 刷新指定学期的开设缓存。
     *
     * @param semester 要刷新的学期
     */
    public void refreshOfferings(String semester) {
        log.info("刷新学期开设缓存: semester={}", semester);
        offeringL1Cache.remove(semester);
        offeringL2View.remove(semester);
    }

    /**
     * 刷新指定课程的缓存。
     *
     * @param courseId 要刷新的课程编号
     */
    public void refreshCourse(String courseId) {
        log.info("刷新课程缓存: courseId={}", courseId);
        courseL1Cache.remove(courseId);
        courseL2View.remove(courseId);
    }

    // ---- 内部方法 ----

    private String buildCoursesCacheKey(List<String> courseIds) {
        return courseIds.stream().sorted().reduce((a, b) -> a + "," + b).orElse("");
    }

    private record L1Entry<T>(T value, long timestamp) {}

    private record L2Entry<T>(T value, long timestamp) {}
}
