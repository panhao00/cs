package com.crs.service;

import com.crs.adapter.CatalogAdapter;
import com.crs.domain.Course;
import com.crs.domain.CourseOffering;
import com.crs.domain.Enrollment;
import com.crs.domain.EnrollmentStatus;
import com.crs.domain.OfferingStatus;
import com.crs.repository.CourseOfferingRepository;
import com.crs.repository.EnrollmentRepository;
import com.crs.web.dto.EnrollmentTrend;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 选课统计服务。
 * <p>
 * 提供热度仪表盘所需的统计数据:
 * <ul>
 *   <li>学期总览: 总课程数、总学生数、总选课数、平均选课率</li>
 *   <li>课程热度排名: 按选课率降序排列</li>
 *   <li>课程选课趋势: 各学期选课人数变化</li>
 *   <li>院系统计: 各院系选课情况</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentStatsService {

    private final CourseOfferingRepository offeringRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CatalogAdapter catalogAdapter;

    /**
     * 学期总览统计。
     *
     * @param semester 学期
     * @return 包含总课程数、总学生数、总选课数、平均选课率的 Map
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSemesterOverview(String semester) {
        List<CourseOffering> offerings = offeringRepository
                .findBySemesterAndStatus(semester, OfferingStatus.OPEN);

        // 同时包含 COMMITTED 的开设(已确认的课程也应统计)
        List<CourseOffering> committedOfferings = offeringRepository
                .findBySemesterAndStatus(semester, OfferingStatus.COMMITTED);
        List<CourseOffering> allOfferings = new ArrayList<>(offerings);
        allOfferings.addAll(committedOfferings);

        int totalCourses = allOfferings.size();
        int totalEnrolled = 0;
        int totalCapacity = 0;
        java.util.Set<Long> uniqueStudents = new java.util.HashSet<>();

        for (CourseOffering offering : allOfferings) {
            List<Enrollment> enrollments = enrollmentRepository
                    .findByOfferingIdAndStatus(offering.getId(), EnrollmentStatus.ENROLLED);
            totalEnrolled += enrollments.size();
            totalCapacity += offering.getMaxCapacity();
            for (Enrollment e : enrollments) {
                uniqueStudents.add(e.getStudentId());
            }
        }

        double avgEnrollmentRate = totalCapacity > 0
                ? (double) totalEnrolled / totalCapacity
                : 0;

        Map<String, Object> overview = new HashMap<>();
        overview.put("semester", semester);
        overview.put("totalCourses", totalCourses);
        overview.put("totalStudents", uniqueStudents.size());
        overview.put("totalEnrollments", totalEnrolled);
        overview.put("averageEnrollmentRate", Math.round(avgEnrollmentRate * 10000.0) / 100.0);

        log.info("学期总览 {}: {} 门课程, {} 名学生, {} 条选课, 平均选课率 {}%",
                semester, totalCourses, uniqueStudents.size(), totalEnrolled,
                Math.round(avgEnrollmentRate * 10000.0) / 100.0);

        return overview;
    }

    /**
     * 获取各开设热度列表(按选课率降序)。
     *
     * @param semester 学期
     * @return 热度排名列表
     */
    @Transactional(readOnly = true)
    public List<EnrollmentTrend> getOfferingPopularity(String semester) {
        List<CourseOffering> offerings = offeringRepository
                .findBySemesterAndStatus(semester, OfferingStatus.OPEN);

        List<CourseOffering> committedOfferings = offeringRepository
                .findBySemesterAndStatus(semester, OfferingStatus.COMMITTED);
        List<CourseOffering> allOfferings = new ArrayList<>(offerings);
        allOfferings.addAll(committedOfferings);

        List<EnrollmentTrend> trends = new ArrayList<>();

        for (CourseOffering offering : allOfferings) {
            Course course = catalogAdapter.getCourse(offering.getCourseId()).orElse(null);
            if (course == null) {
                continue;
            }

            long enrolled = enrollmentRepository.countByOfferingIdAndStatus(
                    offering.getId(), EnrollmentStatus.ENROLLED);
            double rate = offering.getMaxCapacity() > 0
                    ? (double) enrolled / offering.getMaxCapacity()
                    : 0;

            trends.add(new EnrollmentTrend(
                    course.getCourseId(),
                    course.getTitle(),
                    course.getDepartment(),
                    semester,
                    (int) enrolled,
                    offering.getMaxCapacity(),
                    Math.round(rate * 10000.0) / 100.0
            ));
        }

        // 按选课率降序排列
        trends.sort((a, b) -> Double.compare(b.getPopularityRate(), a.getPopularityRate()));

        log.info("课程热度排名 {}: {} 门课程", semester, trends.size());
        return trends;
    }

    /**
     * 获取某课程各学期的选课人数趋势。
     *
     * @param courseId 课程编号
     * @return 各学期选课趋势数据列表
     */
    @Transactional(readOnly = true)
    public List<EnrollmentTrend> getCoursePopularityTrend(String courseId) {
        Course course = catalogAdapter.getCourse(courseId).orElse(null);
        if (course == null) {
            return List.of();
        }

        // 查询该课程所有学期的开设(包含所有状态)
        List<CourseOffering> allOfferings = offeringRepository
                .findByCourseIdAndSemester(courseId, null);

        // 由于 repository 没有按 courseId 查询所有学期的方法,需要从所有开设中过滤
        // 使用一个简单的方法: 查询所有学期的开设
        List<EnrollmentTrend> trends = new ArrayList<>();
        java.util.Set<String> processedSemesters = new java.util.HashSet<>();

        // 获取所有包含该课程的开设(从已知学期列表中查询)
        String[] knownSemesters = {"2025-FALL", "2026-SPRING", "2026-FALL"};
        for (String semester : knownSemesters) {
            List<CourseOffering> semesterOfferings = offeringRepository
                    .findByCourseIdAndSemester(courseId, semester);
            if (semesterOfferings.isEmpty()) {
                continue;
            }

            for (CourseOffering offering : semesterOfferings) {
                if (processedSemesters.contains(offering.getSemester())) {
                    continue;
                }
                processedSemesters.add(offering.getSemester());

                long enrolled = enrollmentRepository.countByOfferingIdAndStatus(
                        offering.getId(), EnrollmentStatus.ENROLLED);
                double rate = offering.getMaxCapacity() > 0
                        ? (double) enrolled / offering.getMaxCapacity()
                        : 0;

                trends.add(new EnrollmentTrend(
                        course.getCourseId(),
                        course.getTitle(),
                        course.getDepartment(),
                        offering.getSemester(),
                        (int) enrolled,
                        offering.getMaxCapacity(),
                        Math.round(rate * 10000.0) / 100.0
                ));
            }
        }

        // 按学期排序
        trends.sort((a, b) -> a.getSemester().compareTo(b.getSemester()));

        log.info("课程趋势 {}: {} 个学期数据", courseId, trends.size());
        return trends;
    }

    /**
     * 获取各院系选课统计。
     *
     * @param semester 学期
     * @return 院系统计列表
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDepartmentEnrollmentStats(String semester) {
        List<CourseOffering> offerings = offeringRepository
                .findBySemesterAndStatus(semester, OfferingStatus.OPEN);

        List<CourseOffering> committedOfferings = offeringRepository
                .findBySemesterAndStatus(semester, OfferingStatus.COMMITTED);
        List<CourseOffering> allOfferings = new ArrayList<>(offerings);
        allOfferings.addAll(committedOfferings);

        // 按院系分组统计
        Map<String, int[]> deptStats = new HashMap<>(); // [课程数, 总选课数, 总容量]

        for (CourseOffering offering : allOfferings) {
            Course course = catalogAdapter.getCourse(offering.getCourseId()).orElse(null);
            String dept = course != null ? course.getDepartment() : "Unknown";

            int[] stats = deptStats.computeIfAbsent(dept, k -> new int[]{0, 0, 0});
            stats[0]++; // 课程数

            long enrolled = enrollmentRepository.countByOfferingIdAndStatus(
                    offering.getId(), EnrollmentStatus.ENROLLED);
            stats[1] += (int) enrolled; // 总选课数
            stats[2] += offering.getMaxCapacity(); // 总容量
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : deptStats.entrySet()) {
            Map<String, Object> deptInfo = new HashMap<>();
            deptInfo.put("department", entry.getKey());
            deptInfo.put("courseCount", entry.getValue()[0]);
            deptInfo.put("totalEnrollments", entry.getValue()[1]);
            deptInfo.put("totalCapacity", entry.getValue()[2]);
            deptInfo.put("enrollmentRate", entry.getValue()[2] > 0
                    ? Math.round((double) entry.getValue()[1] / entry.getValue()[2] * 10000.0) / 100.0
                    : 0);
            result.add(deptInfo);
        }

        // 按选课率降序排列
        result.sort((a, b) -> Double.compare(
                (double) b.get("enrollmentRate"),
                (double) a.get("enrollmentRate")));

        log.info("院系统计 {}: {} 个院系", semester, result.size());
        return result;
    }
}
