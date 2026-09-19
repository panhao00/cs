package com.crs.service;

import com.crs.adapter.CatalogAdapter;
import com.crs.domain.Course;
import com.crs.domain.CourseOffering;
import com.crs.domain.Enrollment;
import com.crs.domain.EnrollmentStatus;
import com.crs.domain.OfferingStatus;
import com.crs.domain.User;
import com.crs.repository.CourseOfferingRepository;
import com.crs.repository.EnrollmentRepository;
import com.crs.repository.UserRepository;
import com.crs.web.dto.CourseOfferingDTO;
import com.crs.web.dto.CourseRecommendation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 智能选课推荐服务。
 * <p>
 * 基于四个维度为学生生成个性化课程推荐:
 * <ol>
 *   <li>先修课程链: 检查学生已修课程,推荐满足先修条件的后续课程</li>
 *   <li>专业匹配: 根据学生所属院系推荐同院系课程</li>
 *   <li>热门推荐: 推荐当前选课人数最多的未修课程</li>
 *   <li>学分平衡: 优先推荐学分数与学生历史平均学分接近的课程</li>
 * </ol>
 * 每个推荐附带推荐理由,按综合推荐分数排序返回。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final CourseOfferingRepository offeringRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final CatalogAdapter catalogAdapter;

    /** 先修课程链维度权重 */
    private static final double WEIGHT_PREREQUISITE = 4.0;
    /** 专业匹配维度权重 */
    private static final double WEIGHT_DEPARTMENT = 3.0;
    /** 热门推荐维度权重 */
    private static final double WEIGHT_POPULARITY = 2.0;
    /** 学分平衡维度权重 */
    private static final double WEIGHT_CREDIT_BALANCE = 1.5;

    /**
     * 获取学生个性化课程推荐。
     *
     * @param studentId 学生 ID
     * @param semester  当前学期
     * @return 按推荐分数降序排列的推荐列表
     */
    @Transactional(readOnly = true)
    public List<CourseRecommendation> getRecommendations(Long studentId, String semester) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("学生不存在: " + studentId));

        // 获取学生已修课程 ID 集合
        Set<String> completedCourseIds = getCompletedCourseIds(studentId);

        // 获取学生本学期已选课程 ID 集合(避免推荐已选课程)
        Set<String> currentSemesterCourseIds = getCurrentSemesterCourseIds(studentId, semester);

        // 合并已修 + 已选,作为"不需要推荐"的集合
        Set<String> excludeCourseIds = new HashSet<>();
        excludeCourseIds.addAll(completedCourseIds);
        excludeCourseIds.addAll(currentSemesterCourseIds);

        // 获取本学期所有 OPEN 状态的开设
        List<CourseOffering> offerings = offeringRepository
                .findBySemesterAndStatus(semester, OfferingStatus.OPEN);

        // 计算学生历史平均学分
        double avgCredits = calculateAverageCredits(completedCourseIds);

        // 构建推荐列表
        List<CourseRecommendation> recommendations = new ArrayList<>();

        for (CourseOffering offering : offerings) {
            Course course = catalogAdapter.getCourse(offering.getCourseId()).orElse(null);
            if (course == null) {
                continue;
            }

            // 跳过已修/已选课程
            if (excludeCourseIds.contains(course.getCourseId())) {
                continue;
            }

            // 跳过无容量的课程
            long enrolled = enrollmentRepository.countByOfferingIdAndStatus(
                    offering.getId(), EnrollmentStatus.ENROLLED);
            if (enrolled >= offering.getMaxCapacity()) {
                continue;
            }

            // 构建 CourseOfferingDTO
            CourseOfferingDTO offeringDTO = buildOfferingDTO(offering, course, (int) enrolled);

            // 计算各维度分数
            double prereqScore = 0;
            double deptScore = 0;
            double popularityScore = 0;
            double creditScore = 0;
            List<String> reasons = new ArrayList<>();

            // 维度1: 先修课程链
            if (course.getPrerequisiteId() != null
                    && completedCourseIds.contains(course.getPrerequisiteId())) {
                prereqScore = WEIGHT_PREREQUISITE;
                Course prereqCourse = catalogAdapter.getCourse(course.getPrerequisiteId()).orElse(null);
                String prereqName = prereqCourse != null ? prereqCourse.getTitle() : course.getPrerequisiteId();
                reasons.add("已完成先修课 " + prereqName);
            } else if (course.getPrerequisiteId() == null) {
                // 无先修课要求,给基础分
                prereqScore = WEIGHT_PREREQUISITE * 0.5;
                reasons.add("无先修课要求,适合选修");
            }

            // 维度2: 专业匹配
            String studentDept = student.getDepartment();
            if (studentDept != null && studentDept.equals(course.getDepartment())) {
                deptScore = WEIGHT_DEPARTMENT;
                reasons.add("与您所属院系 " + studentDept + " 匹配");
            }

            // 维度3: 热门推荐
            if (offering.getMaxCapacity() > 0) {
                double popularityRate = (double) enrolled / offering.getMaxCapacity();
                popularityScore = popularityRate * WEIGHT_POPULARITY;
                if (popularityRate >= 0.7) {
                    reasons.add("热门课程,选课率已达 " + Math.round(popularityRate * 100) + "%");
                } else if (popularityRate >= 0.3) {
                    reasons.add("有一定热度的课程");
                }
            }

            // 维度4: 学分平衡
            if (avgCredits > 0 && course.getCredits() != null) {
                double diff = Math.abs(course.getCredits() - avgCredits);
                creditScore = Math.max(0, WEIGHT_CREDIT_BALANCE * (1 - diff / avgCredits));
                if (diff <= 1) {
                    reasons.add("学分 " + course.getCredits() + " 与您历史平均学分 " + String.format("%.1f", avgCredits) + " 接近");
                }
            } else if (avgCredits == 0) {
                creditScore = WEIGHT_CREDIT_BALANCE * 0.5;
            }

            // 综合分数
            double totalScore = prereqScore + deptScore + popularityScore + creditScore;

            // 如果没有任何推荐理由,给一个默认理由
            if (reasons.isEmpty()) {
                reasons.add("系统推荐课程");
            }

            String reason = String.join("; ", reasons);

            recommendations.add(new CourseRecommendation(
                    offeringDTO,
                    0.0,
                    (int) enrolled,
                    offering.getMaxCapacity(),
                    reason,
                    totalScore
            ));
        }

        // 按推荐分数降序排列
        recommendations.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        log.info("为学生 {} 生成 {} 条推荐(学期: {})", studentId, recommendations.size(), semester);
        return recommendations;
    }

    /**
     * 获取学生已完成的课程 ID 集合。
     * <p>
     * 通过选课记录(ENROLLED 状态)确定学生已修课程。
     */
    private Set<String> getCompletedCourseIds(Long studentId) {
        List<Enrollment> enrollments = enrollmentRepository
                .findByStudentIdAndStatus(studentId, EnrollmentStatus.ENROLLED);
        Set<String> courseIds = new HashSet<>();
        for (Enrollment e : enrollments) {
            offeringRepository.findById(e.getOfferingId()).ifPresent(o -> courseIds.add(o.getCourseId()));
        }
        return courseIds;
    }

    /**
     * 获取学生当前学期已选课程 ID 集合。
     */
    private Set<String> getCurrentSemesterCourseIds(Long studentId, String semester) {
        List<Enrollment> enrollments = enrollmentRepository
                .findByStudentIdAndSemesterAndStatus(studentId, semester, EnrollmentStatus.ENROLLED);
        Set<String> courseIds = new HashSet<>();
        for (Enrollment e : enrollments) {
            offeringRepository.findById(e.getOfferingId()).ifPresent(o -> courseIds.add(o.getCourseId()));
        }
        return courseIds;
    }

    /**
     * 计算学生历史平均学分。
     */
    private double calculateAverageCredits(Set<String> completedCourseIds) {
        if (completedCourseIds.isEmpty()) {
            return 0;
        }
        int totalCredits = 0;
        int count = 0;
        for (String courseId : completedCourseIds) {
            Course course = catalogAdapter.getCourse(courseId).orElse(null);
            if (course != null && course.getCredits() != null) {
                totalCredits += course.getCredits();
                count++;
            }
        }
        return count > 0 ? (double) totalCredits / count : 0;
    }

    /**
     * 构建 CourseOfferingDTO。
     */
    private CourseOfferingDTO buildOfferingDTO(CourseOffering offering, Course course, int enrolledCount) {
        return new CourseOfferingDTO(
                offering.getId(),
                course.getCourseId(),
                course.getTitle(),
                course.getDepartment(),
                course.getCredits(),
                course.getPrerequisiteId(),
                offering.getSemester(),
                offering.getDays(),
                offering.getTimes(),
                offering.getRoom(),
                offering.getMaxCapacity(),
                enrolledCount,
                offering.getMaxCapacity() - enrolledCount,
                offering.getStatus(),
                offering.getProfessorId(),
                null
        );
    }
}
