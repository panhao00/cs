package com.crs.service;

import com.crs.common.ApiException;
import com.crs.domain.Course;
import com.crs.domain.CourseOffering;
import com.crs.domain.Enrollment;
import com.crs.domain.EnrollmentStatus;
import com.crs.domain.Grade;
import com.crs.domain.User;
import com.crs.repository.CourseOfferingRepository;
import com.crs.repository.CourseRepository;
import com.crs.repository.EnrollmentRepository;
import com.crs.repository.GradeRepository;
import com.crs.repository.UserRepository;
import com.crs.web.dto.ReportCardDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 成绩单服务: 实现用例 7.5 View Report Card。
 * <p>
 * 基本流:
 * 1. 学生查看上学期成绩单
 * 2. 系统从 Billing 系统获取该生缴费记录(简化: 假设全部已缴)
 * 3. 系统检索该生上学期所有课程成绩并显示
 * <p>
 * 备选流:
 * - Billing Holds: 有欠费记录 → 报错,用例终止(简化: 未实现)
 * - No Grades Available: 无成绩 → 报错,用例终止
 * <p>
 * 安全: 学生只能查看自己的成绩单(水平越权防护);审计日志记录查看操作。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportCardService {

    private final EnrollmentRepository enrollmentRepository;
    private final GradeRepository gradeRepository;
    private final CourseOfferingRepository offeringRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    /** 上学期 */
    private static final String PREVIOUS_SEMESTER = "2026-SPRING";

    /**
     * 查看学生上学期成绩单(基本流)。
     * 备选流 No Grades Available: 返回空成绩单,前端可提示。
     */
    @Transactional
    public ReportCardDTO getReportCard(Long studentId) {
        // 获取学生信息(用于审计/显示)
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> ApiException.notFound("学生不存在"));

        // 检索上学期所有选课记录(使用带学期过滤的高效查询)
        List<Enrollment> enrollments = enrollmentRepository
                .findByStudentIdAndSemesterAndStatus(studentId, PREVIOUS_SEMESTER, EnrollmentStatus.ENROLLED);

        List<ReportCardDTO.ReportEntry> entries = new ArrayList<>();
        double totalQualityPoints = 0.0;
        int totalCredits = 0;

        for (Enrollment e : enrollments) {
            CourseOffering offering = offeringRepository.findById(e.getOfferingId())
                    .orElse(null);
            if (offering == null) continue;

            Course course = courseRepository.findById(offering.getCourseId()).orElse(null);
            if (course == null) continue;

            Grade grade = gradeRepository
                    .findByOfferingIdAndStudentId(offering.getId(), studentId)
                    .orElse(null);

            // 教授名
            String profName = null;
            if (offering.getProfessorId() != null) {
                profName = userRepository.findById(offering.getProfessorId())
                        .map(User::getName).orElse(null);
            }

            entries.add(new ReportCardDTO.ReportEntry(
                    offering.getId(),
                    offering.getCourseId(),
                    course.getTitle(),
                    course.getDepartment(),
                    course.getCredits(),
                    grade != null ? grade.getGrade() : null,
                    profName
            ));

            // 计算 GPA
            if (grade != null && grade.getGrade() != null) {
                int credits = course.getCredits() != null ? course.getCredits() : 0;
                totalCredits += credits;
                totalQualityPoints += gradeToPoint(grade.getGrade()) * credits;
            }
        }

        double gpa = totalCredits > 0 ? totalQualityPoints / totalCredits : 0.0;

        // 审计日志: 记录学生查看成绩单操作
        log.info("学生 {} ({}) 查看了 {} 学期成绩单, 共 {} 门课程",
                studentId, student.getUsername(), PREVIOUS_SEMESTER, entries.size());

        return new ReportCardDTO(PREVIOUS_SEMESTER, entries, Math.round(gpa * 100.0) / 100.0);
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
}
