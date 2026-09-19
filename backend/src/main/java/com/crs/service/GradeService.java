package com.crs.service;

import com.crs.common.ApiException;
import com.crs.domain.Course;
import com.crs.domain.CourseOffering;
import com.crs.domain.Enrollment;
import com.crs.domain.EnrollmentStatus;
import com.crs.domain.Grade;
import com.crs.domain.OfferingStatus;
import com.crs.domain.User;
import com.crs.repository.CourseOfferingRepository;
import com.crs.repository.CourseRepository;
import com.crs.repository.EnrollmentRepository;
import com.crs.repository.GradeRepository;
import com.crs.repository.UserRepository;
import com.crs.web.dto.StudentGradeDTO;
import com.crs.web.dto.TeachingOfferingDTO;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 成绩服务: 实现用例 7.4 Submit Grades。
 * <p>
 * 基本流:
 * 1. 系统显示教授上学期授课列表
 * 2. 教授选择一门开设
 * 3. 系统检索该班全部选课学生并显示(含已录成绩)
 * 4. 教授为每生录入成绩 A/B/C/D/F/I,可留空稍后补录,可修改
 * <p>
 * 备选流 No Course Offerings Taught: 上学期未授课 → 报错,用例终止
 * <p>
 * 安全: 只有教授能录成绩;只能为自己任教的开设录成绩(水平越权防护)。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GradeService {

    private final CourseOfferingRepository offeringRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final GradeRepository gradeRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final com.crs.common.SemesterConfig semesterConfig;

    private static final Set<String> VALID_GRADES = Set.of("A", "B", "C", "D", "F", "I");

    /** 上学期(从 SemesterConfig 计算,避免硬编码) */
    private String previousSemester() {
        return semesterConfig.getPreviousSemester();
    }

    private String currentSemester() {
        return semesterConfig.getCurrentSemester();
    }

    /**
     * 获取教授上学期授课列表(基本流 1)。
     * 备选流 No Course Offerings Taught: 返回空列表。
     */
    @Transactional(readOnly = true)
    public List<TeachingOfferingDTO> getTaughtOfferings(Long professorId) {
        String sem = previousSemester();
        List<CourseOffering> allByProf = offeringRepository
                .findBySemesterAndProfessorId(sem, professorId);
        if (allByProf.isEmpty()) {
            log.info("教授 {} 在 {} 学期未授课", professorId, sem);
        }
        return allByProf.stream()
                .map(o -> {
                    Course c = courseRepository.findById(o.getCourseId()).orElse(null);
                    long enrolled = enrollmentRepository.countByOfferingIdAndStatus(
                            o.getId(), EnrollmentStatus.ENROLLED);
                    return new TeachingOfferingDTO(
                            o.getId(),
                            o.getCourseId(),
                            c != null ? c.getTitle() : null,
                            c != null ? c.getDepartment() : null,
                            o.getSemester(),
                            o.getDays(),
                            o.getTimes(),
                            o.getRoom(),
                            o.getStatus(),
                            professorId.equals(o.getProfessorId()),
                            (int) enrolled,
                            o.getMaxCapacity()
                    );
                })
                .toList();
    }

    /**
     * 获取某开设的班级名单 + 成绩(基本流 3)。
     * 安全: 必须是该开设的任教教授才能查看。
     */
    @Transactional(readOnly = true)
    public List<StudentGradeDTO> getRosterWithGrades(Long professorId, Long offeringId) {
        CourseOffering offering = offeringRepository.findById(offeringId)
                .orElseThrow(() -> ApiException.notFound("开设不存在"));
        // 水平越权防护
        if (!offering.getProfessorId().equals(professorId)) {
            throw ApiException.forbidden("只能查看自己任教的班级");
        }

        List<Enrollment> enrollments = enrollmentRepository
                .findByOfferingIdAndStatus(offeringId, EnrollmentStatus.ENROLLED);

        return enrollments.stream()
                .map(e -> {
                    User student = userRepository.findById(e.getStudentId()).orElse(null);
                    Grade g = gradeRepository
                            .findByOfferingIdAndStudentId(offeringId, e.getStudentId())
                            .orElse(null);
                    return new StudentGradeDTO(
                            e.getId(),
                            e.getStudentId(),
                            student != null ? student.getName() : null,
                            g != null ? g.getGrade() : null,
                            e.getStatus().name()
                    );
                })
                .toList();
    }

    /**
     * 录入/修改单个学生成绩(基本流 4)。
     * <p>
     * 业务规则: 成绩取值 A/B/C/D/F/I;可留空稍后补录;可修改。
     * 安全: 必须是该开设的任教教授。
     */
    @Transactional
    public StudentGradeDTO submitGrade(Long professorId, Long offeringId,
                                       Long studentId, String grade, boolean clear) {
        CourseOffering offering = offeringRepository.findById(offeringId)
                .orElseThrow(() -> ApiException.notFound("开设不存在"));
        if (!offering.getProfessorId().equals(professorId)) {
            throw ApiException.forbidden("只能为自己任教的班级录入成绩");
        }

        // 校验学生是否在该班级
        Enrollment enrollment = enrollmentRepository
                .findByOfferingIdAndStatus(offeringId, EnrollmentStatus.ENROLLED)
                .stream()
                .filter(e -> e.getStudentId().equals(studentId))
                .findFirst()
                .orElseThrow(() -> ApiException.badRequest("学生未注册此课程"));

        // 校验成绩值
        if (!clear) {
            if (grade == null || grade.isBlank()) {
                throw ApiException.badRequest("成绩不能为空(留空请使用 clear=true)");
            }
            if (!VALID_GRADES.contains(grade.toUpperCase())) {
                throw ApiException.badRequest(
                        "成绩取值必须为 A/B/C/D/F/I,当前: " + grade);
            }
            grade = grade.toUpperCase();
        } else {
            grade = null;
        }

        // 查找或创建成绩记录
        Grade gradeRecord = gradeRepository
                .findByOfferingIdAndStudentId(offeringId, studentId)
                .orElseGet(() -> {
                    Grade g = new Grade();
                    g.setOfferingId(offeringId);
                    g.setStudentId(studentId);
                    return g;
                });

        gradeRecord.setGrade(grade);
        gradeRepository.save(gradeRecord);

        auditService.recordGradeEntry(professorId, offeringId, studentId,
                offering.getCourseId(), grade);

        log.info("教授 {} 为学生 {} 在开设 {} 录入成绩 {}", professorId, studentId, offeringId, grade);

        // 通知学生成绩已录入(7.4 备选流)
        if (grade != null) {
            Course course = courseRepository.findById(offering.getCourseId()).orElse(null);
            String courseTitle = course != null ? course.getCourseId() : offering.getCourseId();
            notificationService.notifyGradePosted(studentId, offeringId, courseTitle, grade);
        }

        User student = userRepository.findById(studentId).orElse(null);
        return new StudentGradeDTO(
                enrollment.getId(),
                studentId,
                student != null ? student.getName() : null,
                grade,
                enrollment.getStatus().name()
        );
    }
}
