package com.crs.service;

import com.crs.common.ApiException;
import com.crs.domain.Course;
import com.crs.domain.CourseOffering;
import com.crs.domain.Enrollment;
import com.crs.domain.EnrollmentStatus;
import com.crs.domain.Grade;
import com.crs.repository.CourseOfferingRepository;
import com.crs.repository.CourseRepository;
import com.crs.repository.EnrollmentRepository;
import com.crs.repository.GradeRepository;
import com.crs.web.dto.CreditSummary;
import com.crs.web.dto.GpaTrend;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 成绩统计服务: GPA 趋势分析、学分统计、成绩分布。
 * <p>
 * 为学生提供学业进展可视化数据,为教授提供班级成绩分布。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GradeStatsService {

    private final GradeRepository gradeRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseOfferingRepository offeringRepository;
    private final CourseRepository courseRepository;

    /**
     * 获取学生所有学期的 GPA 趋势列表(按学期正序排列)。
     * <p>
     * 每个学期计算: 该学期 GPA(加权平均)、学分总和、课程数量。
     */
    @Transactional(readOnly = true)
    public List<GpaTrend> getGpaTrend(Long studentId) {
        // 获取学生所有已选课的记录
        List<Enrollment> enrollments = enrollmentRepository
                .findByStudentIdAndStatus(studentId, EnrollmentStatus.ENROLLED);

        // 按学期分组
        Map<String, List<EnroffingWithGrade>> semesterMap = new LinkedHashMap<>();

        for (Enrollment e : enrollments) {
            CourseOffering offering = offeringRepository.findById(e.getOfferingId())
                    .orElse(null);
            if (offering == null) continue;

            Course course = courseRepository.findById(offering.getCourseId()).orElse(null);
            if (course == null) continue;

            Grade grade = gradeRepository
                    .findByOfferingIdAndStudentId(offering.getId(), studentId)
                    .orElse(null);

            String semester = offering.getSemester();
            semesterMap.computeIfAbsent(semester, k -> new ArrayList<>())
                    .add(new EnroffingWithGrade(offering, course, grade));
        }

        // 按学期正序排列,计算每个学期的 GPA
        List<GpaTrend> result = new ArrayList<>();
        for (Map.Entry<String, List<EnroffingWithGrade>> entry : semesterMap.entrySet()) {
            String semester = entry.getKey();
            List<EnroffingWithGrade> items = entry.getValue();

            double totalQualityPoints = 0.0;
            int totalCredits = 0;
            int courseCount = 0;

            for (EnroffingWithGrade item : items) {
                courseCount++;
                int credits = item.course.getCredits() != null ? item.course.getCredits() : 0;
                if (item.grade != null && item.grade.getGrade() != null) {
                    totalCredits += credits;
                    totalQualityPoints += gradeToPoint(item.grade.getGrade()) * credits;
                }
            }

            double gpa = totalCredits > 0
                    ? Math.round(totalQualityPoints / totalCredits * 100.0) / 100.0
                    : 0.0;

            result.add(new GpaTrend(semester, gpa, totalCredits, courseCount));
        }

        return result;
    }

    /**
     * 获取学分统计。
     * <p>
     * 总学分(所有已选课程), 已修学分(有成绩的), 平均 GPA, 按院系学分分布。
     */
    @Transactional(readOnly = true)
    public CreditSummary getCreditSummary(Long studentId) {
        List<Enrollment> enrollments = enrollmentRepository
                .findByStudentIdAndStatus(studentId, EnrollmentStatus.ENROLLED);

        int totalCredits = 0;
        int completedCredits = 0;
        double totalQualityPoints = 0.0;
        Map<String, Integer> departmentCredits = new LinkedHashMap<>();

        for (Enrollment e : enrollments) {
            CourseOffering offering = offeringRepository.findById(e.getOfferingId())
                    .orElse(null);
            if (offering == null) continue;

            Course course = courseRepository.findById(offering.getCourseId()).orElse(null);
            if (course == null) continue;

            int credits = course.getCredits() != null ? course.getCredits() : 0;
            totalCredits += credits;

            // 按院系统计
            String dept = course.getDepartment() != null ? course.getDepartment() : "Other";
            departmentCredits.merge(dept, credits, Integer::sum);

            // 有成绩的课程
            Grade grade = gradeRepository
                    .findByOfferingIdAndStudentId(offering.getId(), studentId)
                    .orElse(null);
            if (grade != null && grade.getGrade() != null) {
                completedCredits += credits;
                totalQualityPoints += gradeToPoint(grade.getGrade()) * credits;
            }
        }

        double averageGpa = completedCredits > 0
                ? Math.round(totalQualityPoints / completedCredits * 100.0) / 100.0
                : 0.0;

        return new CreditSummary(totalCredits, completedCredits, averageGpa, departmentCredits);
    }

    /**
     * 获取某开设的成绩分布。
     * <p>
     * 返回 A/B/C/D/F/I 各多少人及其百分比。
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getGradeDistribution(Long offeringId) {
        // 校验开设存在
        offeringRepository.findById(offeringId)
                .orElseThrow(() -> ApiException.notFound("课程开设不存在: " + offeringId));

        List<Grade> grades = gradeRepository.findByOfferingId(offeringId);
        int totalStudents = grades.size();

        // 初始化分布
        Map<String, Integer> countMap = new LinkedHashMap<>();
        for (String g : List.of("A", "B", "C", "D", "F", "I")) {
            countMap.put(g, 0);
        }

        for (Grade grade : grades) {
            String g = grade.getGrade();
            if (g != null && countMap.containsKey(g)) {
                countMap.merge(g, 1, Integer::sum);
            }
        }

        // 构建结果: 包含人数和百分比
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalStudents", totalStudents);

        List<Map<String, Object>> distribution = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : countMap.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("grade", entry.getKey());
            item.put("count", entry.getValue());
            item.put("percentage", totalStudents > 0
                    ? Math.round(entry.getValue() * 100.0 / totalStudents * 10.0) / 10.0
                    : 0.0);
            distribution.add(item);
        }
        result.put("distribution", distribution);

        return result;
    }

    /** 成绩转绩点 */
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

    /** 内部记录: 选课 + 课程 + 成绩 */
    private record EnroffingWithGrade(
            CourseOffering offering,
            Course course,
            Grade grade
    ) {}
}
