package com.crs.service;

import com.crs.common.SemesterConfig;
import com.crs.domain.Course;
import com.crs.domain.CourseOffering;
import com.crs.domain.Enrollment;
import com.crs.domain.EnrollmentStatus;
import com.crs.domain.Grade;
import com.crs.repository.CourseOfferingRepository;
import com.crs.repository.CourseRepository;
import com.crs.repository.EnrollmentRepository;
import com.crs.repository.GradeRepository;
import com.crs.web.dto.AcademicSummaryDTO;
import com.crs.web.dto.AcademicSummaryDTO.CourseGrade;
import com.crs.web.dto.AcademicSummaryDTO.SemesterEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 学业摘要服务: 聚合学生的累计 GPA、学分统计和按学期分组的课程历史。
 * <p>
 * 数据来源:
 * - Grade: 成绩记录(通过 offeringId 关联 CourseOffering 获取学期)
 * - CourseOffering: 课程开设(提供 courseId 和 semester)
 * - Course: 课程信息(提供 credits 和 title)
 * - Enrollment: 选课记录(统计当前学期选课学分)
 * <p>
 * 安全: 学生只能查看自己的学业摘要(水平越权防护在 Controller 层由 RoleGuard 保证)。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AcademicSummaryService {

    private final GradeRepository gradeRepository;
    private final CourseOfferingRepository offeringRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SemesterConfig semesterConfig;

    /**
     * 获取学生学业摘要。
     * <p>
     * 处理流程:
     * 1. 查询该生所有成绩记录
     * 2. 批量查询对应的 CourseOffering(获取 courseId 和 semester)
     * 3. 批量查询对应的 Course(获取 credits 和 title)
     * 4. 按学期分组,计算每学期 GPA 和累计 GPA
     * 5. 查询当前学期选课学分
     * 6. 组装返回 AcademicSummaryDTO
     *
     * @param studentId 学生 ID
     * @return 学业摘要 DTO
     */
    @Transactional(readOnly = true)
    public AcademicSummaryDTO getSummary(Long studentId) {
        // 1. 查询该生所有成绩记录
        List<Grade> grades = gradeRepository.findByStudentId(studentId);

        // 无成绩记录时返回空摘要
        if (grades.isEmpty()) {
            log.info("学生 {} 暂无成绩记录,返回空摘要", studentId);
            return buildEmptySummary(studentId);
        }

        // 2. 批量查询 CourseOffering(去重)
        Set<Long> offeringIds = grades.stream()
                .map(Grade::getOfferingId)
                .collect(Collectors.toSet());
        List<CourseOffering> offerings = offeringRepository.findByIdIn(new ArrayList<>(offeringIds));
        Map<Long, CourseOffering> offeringMap = offerings.stream()
                .collect(Collectors.toMap(CourseOffering::getId, o -> o));

        // 3. 批量查询 Course(去重)
        Set<String> courseIds = offerings.stream()
                .map(CourseOffering::getCourseId)
                .collect(Collectors.toSet());
        List<Course> courses = courseRepository.findAllById(courseIds);
        Map<String, Course> courseMap = courses.stream()
                .collect(Collectors.toMap(Course::getCourseId, c -> c));

        // 4. 按学期分组,计算 GPA 和学分
        Map<String, List<CourseGrade>> semesterCourses = new LinkedHashMap<>();
        double totalQualityPoints = 0.0;
        int totalCreditsAttempted = 0;
        int totalCreditsEarned = 0;

        for (Grade grade : grades) {
            CourseOffering offering = offeringMap.get(grade.getOfferingId());
            if (offering == null) continue;

            Course course = courseMap.get(offering.getCourseId());
            if (course == null) continue;

            String semester = offering.getSemester();
            int credits = course.getCredits() != null ? course.getCredits() : 0;
            String gradeValue = grade.getGrade();
            double qualityPoints = gradeToPoint(gradeValue) * credits;

            CourseGrade courseGrade = new CourseGrade(
                    course.getCourseId(),
                    course.getTitle(),
                    credits,
                    gradeValue,
                    Math.round(qualityPoints * 100.0) / 100.0
            );

            semesterCourses.computeIfAbsent(semester, k -> new ArrayList<>()).add(courseGrade);

            // 累计统计(所有有成绩的课程)
            totalCreditsAttempted += credits;
            totalQualityPoints += qualityPoints;

            // 已获得学分(成绩为 A/B/C/D)
            if (isPassingGrade(gradeValue)) {
                totalCreditsEarned += credits;
            }
        }

        // 5. 构建学期历史(逆序: 最新学期在前)
        List<SemesterEntry> semesterHistory = new ArrayList<>();
        for (Map.Entry<String, List<CourseGrade>> entry : semesterCourses.entrySet()) {
            String semester = entry.getKey();
            List<CourseGrade> semesterGrades = entry.getValue();

            double semesterQualityPoints = semesterGrades.stream()
                    .mapToDouble(CourseGrade::getQualityPoints)
                    .sum();
            int semesterCredits = semesterGrades.stream()
                    .mapToInt(CourseGrade::getCredits)
                    .sum();
            double semesterGpa = semesterCredits > 0
                    ? Math.round((semesterQualityPoints / semesterCredits) * 100.0) / 100.0
                    : 0.0;

            semesterHistory.add(new SemesterEntry(
                    semester,
                    semesterGpa,
                    semesterCredits,
                    semesterGrades
            ));
        }

        // 逆序: 最新学期在前
        semesterHistory.sort(Comparator.comparing(SemesterEntry::getSemester).reversed());

        // 6. 计算累计 GPA
        double cumulativeGpa = totalCreditsAttempted > 0
                ? Math.round((totalQualityPoints / totalCreditsAttempted) * 100.0) / 100.0
                : 0.0;

        // 7. 查询当前学期选课学分
        int currentEnrolledCredits = calculateCurrentEnrolledCredits(studentId);

        log.info("学生 {} 学业摘要: 累计 GPA={}, 已获得学分={}, 已修读学分={}, 当前选课学分={}, 历史学期数={}",
                studentId, cumulativeGpa, totalCreditsEarned, totalCreditsAttempted,
                currentEnrolledCredits, semesterHistory.size());

        return new AcademicSummaryDTO(
                cumulativeGpa,
                totalCreditsEarned,
                totalCreditsAttempted,
                currentEnrolledCredits,
                semesterHistory
        );
    }

    /**
     * 计算当前学期选课学分。
     * 查询当前学期状态为 ENROLLED 的选课记录,汇总对应课程的学分。
     */
    private int calculateCurrentEnrolledCredits(Long studentId) {
        String currentSemester = semesterConfig.getCurrentSemester();
        List<Enrollment> currentEnrollments = enrollmentRepository
                .findByStudentIdAndStatus(studentId, EnrollmentStatus.ENROLLED);

        // 筛选当前学期的选课
        Set<Long> currentOfferingIds = currentEnrollments.stream()
                .map(Enrollment::getOfferingId)
                .collect(Collectors.toSet());

        if (currentOfferingIds.isEmpty()) {
            return 0;
        }

        List<CourseOffering> currentOfferings = offeringRepository.findByIdIn(
                new ArrayList<>(currentOfferingIds));

        Set<String> currentCourseIds = currentOfferings.stream()
                .filter(o -> currentSemester.equals(o.getSemester()))
                .map(CourseOffering::getCourseId)
                .collect(Collectors.toSet());

        if (currentCourseIds.isEmpty()) {
            return 0;
        }

        List<Course> currentCourses = courseRepository.findAllById(currentCourseIds);
        return currentCourses.stream()
                .mapToInt(c -> c.getCredits() != null ? c.getCredits() : 0)
                .sum();
    }

    /**
     * 构建空的学业摘要(学生暂无成绩记录时使用)。
     */
    private AcademicSummaryDTO buildEmptySummary(Long studentId) {
        int currentEnrolledCredits = calculateCurrentEnrolledCredits(studentId);
        return new AcademicSummaryDTO(
                0.0,
                0,
                0,
                currentEnrolledCredits,
                List.of()
        );
    }

    /** 判断成绩是否为通过(A/B/C/D) */
    private boolean isPassingGrade(String grade) {
        return "A".equals(grade) || "B".equals(grade)
                || "C".equals(grade) || "D".equals(grade);
    }

    /** 成绩转绩点(与 ReportCardService 保持一致) */
    private double gradeToPoint(String grade) {
        return switch (grade) {
            case "A" -> 4.0;
            case "B" -> 3.0;
            case "C" -> 2.0;
            case "D" -> 1.0;
            case "F", "I" -> 0.0;
            default -> 0.0;
        };
    }
}
