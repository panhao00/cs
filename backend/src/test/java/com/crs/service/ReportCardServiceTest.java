package com.crs.service;

import com.crs.common.ApiException;
import com.crs.domain.BillStatus;
import com.crs.domain.Course;
import com.crs.domain.CourseOffering;
import com.crs.domain.Enrollment;
import com.crs.domain.EnrollmentStatus;
import com.crs.domain.Grade;
import com.crs.domain.User;
import com.crs.repository.BillRepository;
import com.crs.repository.CourseOfferingRepository;
import com.crs.repository.CourseRepository;
import com.crs.repository.EnrollmentRepository;
import com.crs.repository.GradeRepository;
import com.crs.repository.UserRepository;
import com.crs.web.dto.ReportCardDTO;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ReportCardService 单元测试。
 * <p>
 * 覆盖用例 7.5 View Report Card 的核心业务:
 * - 成绩单查询与 GPA 计算
 * - 空成绩单处理
 * - 数据完整性校验
 */
@ExtendWith(MockitoExtension.class)
class ReportCardServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private GradeRepository gradeRepository;

    @Mock
    private CourseOfferingRepository offeringRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BillRepository billRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ReportCardService reportCardService;

    private static final String SEMESTER = "2026-SPRING";
    private static final Long STUDENT_ID = 1L;
    private static final Long PROFESSOR_ID = 10L;

    private User student;
    private User professor;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(STUDENT_ID);
        student.setUsername("charlie");
        student.setRole(com.crs.domain.Role.STUDENT);
        student.setName("Charlie Brown");

        professor = new User();
        professor.setId(PROFESSOR_ID);
        professor.setUsername("prof_smith");
        professor.setRole(com.crs.domain.Role.PROFESSOR);
        professor.setName("Bob Smith");

        // 默认: 无未缴清账单(正常通过 arrears 检查)
        lenient().when(billRepository.existsByStudentIdAndStatus(STUDENT_ID, BillStatus.PENDING)).thenReturn(false);
        lenient().when(billRepository.existsByStudentIdAndStatus(STUDENT_ID, BillStatus.FAILED)).thenReturn(false);
    }

    // ==================== getReportCard ====================

    @Test
    @DisplayName("查看成绩单 - 无选课记录返回空报告")
    void getReportCard_noEnrollments_returnsEmptyReport() {
        // Arrange
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(enrollmentRepository.findByStudentIdAndSemesterAndStatus(
                STUDENT_ID, SEMESTER, EnrollmentStatus.ENROLLED))
                .thenReturn(List.of());

        // Act
        ReportCardDTO result = reportCardService.getReportCard(STUDENT_ID);

        // Assert
        assertNotNull(result);
        assertEquals(SEMESTER, result.getSemester());
        assertTrue(result.getEntries().isEmpty());
        assertEquals(0.0, result.getGpa(), 0.001);
    }

    @Test
    @DisplayName("查看成绩单 - 学生不存在抛出 404")
    void getReportCard_nonExistentStudent_throwsNotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ApiException ex = assertThrows(ApiException.class, () ->
                reportCardService.getReportCard(999L));
        assertEquals(404, ex.getStatus());
    }

    @Test
    @DisplayName("查看成绩单 - 返回正确课程条目")
    void getReportCard_withGrades_returnsCorrectEntries() {
        // Arrange
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));

        // 1 门选课
        Enrollment enrollment = createEnrollment(1L, 100L, STUDENT_ID);
        when(enrollmentRepository.findByStudentIdAndSemesterAndStatus(
                STUDENT_ID, SEMESTER, EnrollmentStatus.ENROLLED))
                .thenReturn(List.of(enrollment));

        // CourseOffering
        CourseOffering offering = createOffering(100L, "CS101", PROFESSOR_ID);
        when(offeringRepository.findById(100L)).thenReturn(Optional.of(offering));

        // Course
        Course course = createCourse("CS101", "Intro to CS", "Computer Science", 3);
        when(courseRepository.findById("CS101")).thenReturn(Optional.of(course));

        // Grade
        Grade grade = createGrade(200L, 100L, STUDENT_ID, "A");
        when(gradeRepository.findByOfferingIdAndStudentId(100L, STUDENT_ID))
                .thenReturn(Optional.of(grade));

        // Professor name
        when(userRepository.findById(PROFESSOR_ID)).thenReturn(Optional.of(professor));

        // Act
        ReportCardDTO result = reportCardService.getReportCard(STUDENT_ID);

        // Assert
        assertEquals(1, result.getEntries().size());
        ReportCardDTO.ReportEntry entry = result.getEntries().get(0);
        assertEquals("CS101", entry.getCourseId());
        assertEquals("Intro to CS", entry.getCourseTitle());
        assertEquals("Computer Science", entry.getDepartment());
        assertEquals(3, entry.getCredits());
        assertEquals("A", entry.getGrade());
        assertEquals("Bob Smith", entry.getProfessorName());
    }

    @Test
    @DisplayName("查看成绩单 - GPA 计算正确 (单门 A 课程)")
    void getReportCard_singleA_GPA4point0() {
        // Arrange
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));

        Enrollment enrollment = createEnrollment(1L, 100L, STUDENT_ID);
        when(enrollmentRepository.findByStudentIdAndSemesterAndStatus(
                STUDENT_ID, SEMESTER, EnrollmentStatus.ENROLLED))
                .thenReturn(List.of(enrollment));

        CourseOffering offering = createOffering(100L, "CS101", PROFESSOR_ID);
        when(offeringRepository.findById(100L)).thenReturn(Optional.of(offering));

        Course course = createCourse("CS101", "Intro to CS", "CS", 3);
        when(courseRepository.findById("CS101")).thenReturn(Optional.of(course));

        Grade grade = createGrade(200L, 100L, STUDENT_ID, "A");
        when(gradeRepository.findByOfferingIdAndStudentId(100L, STUDENT_ID))
                .thenReturn(Optional.of(grade));
        when(userRepository.findById(PROFESSOR_ID)).thenReturn(Optional.of(professor));

        // Act
        ReportCardDTO result = reportCardService.getReportCard(STUDENT_ID);

        // Assert: A = 4.0 * 3 credits / 3 credits = 4.0
        assertEquals(4.0, result.getGpa(), 0.01);
    }

    @Test
    @DisplayName("查看成绩单 - GPA 计算正确 (混合成绩: A 3学分 + B 4学分)")
    void getReportCard_mixedGrades_calculatesCorrectGPA() {
        // Arrange
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));

        // 两门选课: CS101 (3 credits, A), MATH101 (4 credits, B)
        Enrollment e1 = createEnrollment(1L, 100L, STUDENT_ID);
        Enrollment e2 = createEnrollment(2L, 200L, STUDENT_ID);
        when(enrollmentRepository.findByStudentIdAndSemesterAndStatus(
                STUDENT_ID, SEMESTER, EnrollmentStatus.ENROLLED))
                .thenReturn(List.of(e1, e2));

        // CS101 offering
        CourseOffering off1 = createOffering(100L, "CS101", PROFESSOR_ID);
        when(offeringRepository.findById(100L)).thenReturn(Optional.of(off1));
        Course cs101 = createCourse("CS101", "Intro to CS", "CS", 3);
        when(courseRepository.findById("CS101")).thenReturn(Optional.of(cs101));
        Grade g1 = createGrade(200L, 100L, STUDENT_ID, "A");
        when(gradeRepository.findByOfferingIdAndStudentId(100L, STUDENT_ID))
                .thenReturn(Optional.of(g1));

        // MATH101 offering
        CourseOffering off2 = createOffering(200L, "MATH101", PROFESSOR_ID);
        when(offeringRepository.findById(200L)).thenReturn(Optional.of(off2));
        Course math = createCourse("MATH101", "Calculus I", "Math", 4);
        when(courseRepository.findById("MATH101")).thenReturn(Optional.of(math));
        Grade g2 = createGrade(201L, 200L, STUDENT_ID, "B");
        when(gradeRepository.findByOfferingIdAndStudentId(200L, STUDENT_ID))
                .thenReturn(Optional.of(g2));

        when(userRepository.findById(PROFESSOR_ID)).thenReturn(Optional.of(professor));

        // Act
        ReportCardDTO result = reportCardService.getReportCard(STUDENT_ID);

        // Assert: GPA = (4.0*3 + 3.0*4) / (3+4) = (12+12)/7 = 24/7 ≈ 3.43
        assertEquals(3.43, result.getGpa(), 0.01);
    }

    @Test
    @DisplayName("查看成绩单 - 成绩为 null 时不计入 GPA")
    void getReportCard_nullGrade_excludedFromGPA() {
        // Arrange
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));

        Enrollment e1 = createEnrollment(1L, 100L, STUDENT_ID);
        when(enrollmentRepository.findByStudentIdAndSemesterAndStatus(
                STUDENT_ID, SEMESTER, EnrollmentStatus.ENROLLED))
                .thenReturn(List.of(e1));

        CourseOffering off1 = createOffering(100L, "CS101", PROFESSOR_ID);
        when(offeringRepository.findById(100L)).thenReturn(Optional.of(off1));
        Course cs101 = createCourse("CS101", "Intro to CS", "CS", 3);
        when(courseRepository.findById("CS101")).thenReturn(Optional.of(cs101));

        // 教授未录入成绩
        when(gradeRepository.findByOfferingIdAndStudentId(100L, STUDENT_ID))
                .thenReturn(Optional.empty());
        when(userRepository.findById(PROFESSOR_ID)).thenReturn(Optional.of(professor));

        // Act
        ReportCardDTO result = reportCardService.getReportCard(STUDENT_ID);

        // Assert: 无成绩 → GPA = 0
        assertEquals(0.0, result.getGpa(), 0.001);
        assertEquals(1, result.getEntries().size());
        assertNull(result.getEntries().get(0).getGrade());
    }

    @Test
    @DisplayName("查看成绩单 - 含 D 和 F 成绩的 GPA 计算")
    void getReportCard_withDF_gradesCalculatesCorrectly() {
        // Arrange
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));

        // 3 门课: A(3cr), D(3cr), F(3cr)
        Enrollment e1 = createEnrollment(1L, 100L, STUDENT_ID);
        Enrollment e2 = createEnrollment(2L, 200L, STUDENT_ID);
        Enrollment e3 = createEnrollment(3L, 300L, STUDENT_ID);
        when(enrollmentRepository.findByStudentIdAndSemesterAndStatus(
                STUDENT_ID, SEMESTER, EnrollmentStatus.ENROLLED))
                .thenReturn(List.of(e1, e2, e3));

        CourseOffering off1 = createOffering(100L, "CS101", PROFESSOR_ID);
        when(offeringRepository.findById(100L)).thenReturn(Optional.of(off1));
        Course cs101 = createCourse("CS101", "Intro to CS", "CS", 3);
        when(courseRepository.findById("CS101")).thenReturn(Optional.of(cs101));
        Grade g1 = createGrade(200L, 100L, STUDENT_ID, "A");
        when(gradeRepository.findByOfferingIdAndStudentId(100L, STUDENT_ID))
                .thenReturn(Optional.of(g1));

        CourseOffering off2 = createOffering(200L, "MATH101", PROFESSOR_ID);
        when(offeringRepository.findById(200L)).thenReturn(Optional.of(off2));
        Course math = createCourse("MATH101", "Calculus I", "Math", 3);
        when(courseRepository.findById("MATH101")).thenReturn(Optional.of(math));
        Grade g2 = createGrade(201L, 200L, STUDENT_ID, "D");
        when(gradeRepository.findByOfferingIdAndStudentId(200L, STUDENT_ID))
                .thenReturn(Optional.of(g2));

        CourseOffering off3 = createOffering(300L, "ENG101", PROFESSOR_ID);
        when(offeringRepository.findById(300L)).thenReturn(Optional.of(off3));
        Course eng = createCourse("ENG101", "English Comp", "English", 3);
        when(courseRepository.findById("ENG101")).thenReturn(Optional.of(eng));
        Grade g3 = createGrade(202L, 300L, STUDENT_ID, "F");
        when(gradeRepository.findByOfferingIdAndStudentId(300L, STUDENT_ID))
                .thenReturn(Optional.of(g3));

        when(userRepository.findById(PROFESSOR_ID)).thenReturn(Optional.of(professor));

        // Act
        ReportCardDTO result = reportCardService.getReportCard(STUDENT_ID);

        // Assert: GPA = (4.0*3 + 1.0*3 + 0.0*3) / 9 = 15/9 ≈ 1.67
        assertEquals(1.67, result.getGpa(), 0.01);
        assertEquals(3, result.getEntries().size());
    }

    @Test
    @DisplayName("查看成绩单 - 有未缴清账单时拒绝查看 (PENDING)")
    void getReportCard_pendingBill_throwsForbidden() {
        // Arrange: 存在 PENDING 账单
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(billRepository.existsByStudentIdAndStatus(STUDENT_ID, BillStatus.PENDING)).thenReturn(true);

        // Act & Assert
        ApiException ex = assertThrows(ApiException.class, () ->
                reportCardService.getReportCard(STUDENT_ID));
        assertEquals(403, ex.getStatus());
        assertEquals("有未缴清的账单，成绩单暂不可用", ex.getMessage());
        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    @DisplayName("查看成绩单 - 有未缴清账单时拒绝查看 (FAILED)")
    void getReportCard_failedBill_throwsForbidden() {
        // Arrange: 存在 FAILED 账单
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(billRepository.existsByStudentIdAndStatus(STUDENT_ID, BillStatus.PENDING)).thenReturn(false);
        when(billRepository.existsByStudentIdAndStatus(STUDENT_ID, BillStatus.FAILED)).thenReturn(true);

        // Act & Assert
        ApiException ex = assertThrows(ApiException.class, () ->
                reportCardService.getReportCard(STUDENT_ID));
        assertEquals(403, ex.getStatus());
        assertEquals("有未缴清的账单，成绩单暂不可用", ex.getMessage());
        verifyNoInteractions(enrollmentRepository);
    }

    // ==================== 辅助方法 ====================

    private Enrollment createEnrollment(Long id, Long offeringId, Long studentId) {
        Enrollment enrollment = new Enrollment();
        enrollment.setId(id);
        enrollment.setOfferingId(offeringId);
        enrollment.setStudentId(studentId);
        enrollment.setStatus(EnrollmentStatus.ENROLLED);
        return enrollment;
    }

    private CourseOffering createOffering(Long id, String courseId, Long professorId) {
        CourseOffering offering = new CourseOffering();
        offering.setId(id);
        offering.setCourseId(courseId);
        offering.setSemester(SEMESTER);
        offering.setProfessorId(professorId);
        offering.setMaxCapacity(10);
        return offering;
    }

    private Course createCourse(String courseId, String title, String department, int credits) {
        Course course = new Course();
        course.setCourseId(courseId);
        course.setTitle(title);
        course.setDepartment(department);
        course.setCredits(credits);
        return course;
    }

    private Grade createGrade(Long id, Long offeringId, Long studentId, String grade) {
        Grade g = new Grade();
        g.setId(id);
        g.setOfferingId(offeringId);
        g.setStudentId(studentId);
        g.setGrade(grade);
        return g;
    }
}
