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
 * GradeService 单元测试。
 * <p>
 * 测试用例 7.4 Submit Grades 的核心功能:
 * - 成绩验证 (A/B/C/D/F/I)
 * - 授权检查 (教授只能为自己任教的开设录成绩)
 * - 班级名单 + 成绩查询
 * - 成绩录入/修改 + 通知
 */
@ExtendWith(MockitoExtension.class)
class GradeServiceTest {

    @Mock
    private CourseOfferingRepository offeringRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private GradeRepository gradeRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditService auditService;

    @Mock
    private com.crs.common.SemesterConfig semesterConfig;

    @InjectMocks
    private GradeService gradeService;

    private static final Long PROFESSOR_ID = 1L;
    private static final Long OTHER_PROFESSOR_ID = 99L;
    private static final Long OFFERING_ID = 10L;
    private static final Long STUDENT_ID = 20L;
    private static final Long ENROLLMENT_ID = 30L;
    private static final String COURSE_ID = "CS101";
    private static final String PREVIOUS_SEMESTER = "2026-SPRING";

    private CourseOffering offering;
    private Course course;
    private User professor;
    private User student;
    private Enrollment enrollment;

    @BeforeEach
    void setUp() {
        // 教授
        professor = new User();
        professor.setId(PROFESSOR_ID);
        professor.setUsername("professor");
        professor.setName("Bob Smith");
        professor.setRole(com.crs.domain.Role.PROFESSOR);

        // 另一位教授
        User otherProfessor = new User();
        otherProfessor.setId(OTHER_PROFESSOR_ID);
        otherProfessor.setUsername("otherProf");
        otherProfessor.setName("Other Professor");
        otherProfessor.setRole(com.crs.domain.Role.PROFESSOR);

        // 学生
        student = new User();
        student.setId(STUDENT_ID);
        student.setUsername("student");
        student.setName("Charlie Brown");
        student.setRole(com.crs.domain.Role.STUDENT);

        // 课程
        course = new Course();
        course.setCourseId(COURSE_ID);
        course.setTitle("Intro to CS");
        course.setDepartment("Computer Science");

        // 课程开设
        offering = new CourseOffering();
        offering.setId(OFFERING_ID);
        offering.setCourseId(COURSE_ID);
        offering.setSemester(PREVIOUS_SEMESTER);
        offering.setProfessorId(PROFESSOR_ID);
        offering.setStatus(OfferingStatus.OPEN);
        offering.setMaxCapacity(10);

        // 选课记录
        enrollment = new Enrollment();
        enrollment.setId(ENROLLMENT_ID);
        enrollment.setOfferingId(OFFERING_ID);
        enrollment.setStudentId(STUDENT_ID);
        enrollment.setStatus(EnrollmentStatus.ENROLLED);

        // Mock SemesterConfig
        lenient().when(semesterConfig.getPreviousSemester()).thenReturn(PREVIOUS_SEMESTER);
        lenient().when(semesterConfig.getCurrentSemester()).thenReturn("2026-FALL");
    }

    // ==================== getTaughtOfferings 测试 ====================

    @Test
    @DisplayName("获取教授授课列表 - 有授课时返回正确列表")
    void getTaughtOfferings_withOfferings_returnsCorrectList() {
        // Arrange
        when(offeringRepository.findBySemesterAndProfessorId(PREVIOUS_SEMESTER, PROFESSOR_ID))
                .thenReturn(List.of(offering));
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(enrollmentRepository.countByOfferingIdAndStatus(OFFERING_ID, EnrollmentStatus.ENROLLED))
                .thenReturn(5L);

        // Act
        List<TeachingOfferingDTO> result = gradeService.getTaughtOfferings(PROFESSOR_ID);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(OFFERING_ID, result.get(0).getOfferingId());
        assertEquals(COURSE_ID, result.get(0).getCourseId());
        assertEquals("Intro to CS", result.get(0).getCourseTitle());
        assertEquals("Computer Science", result.get(0).getDepartment());
        assertEquals(PREVIOUS_SEMESTER, result.get(0).getSemester());
        assertEquals(5, result.get(0).getEnrolledCount());
        assertTrue(result.get(0).isSelectedByMe());
        verify(offeringRepository).findBySemesterAndProfessorId(PREVIOUS_SEMESTER, PROFESSOR_ID);
    }

    @Test
    @DisplayName("获取教授授课列表 - 未授课时返回空列表")
    void getTaughtOfferings_noOfferings_returnsEmptyList() {
        // Arrange
        when(offeringRepository.findBySemesterAndProfessorId(PREVIOUS_SEMESTER, PROFESSOR_ID))
                .thenReturn(List.of());

        // Act
        List<TeachingOfferingDTO> result = gradeService.getTaughtOfferings(PROFESSOR_ID);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(offeringRepository).findBySemesterAndProfessorId(PREVIOUS_SEMESTER, PROFESSOR_ID);
        verify(courseRepository, never()).findById(anyString());
    }

    // ==================== getRosterWithGrades 测试 ====================

    @Test
    @DisplayName("获取班级名单 - 返回已注册学生及其成绩")
    void getRosterWithGrades_validRequest_returnsEnrolledStudentsWithGrades() {
        // Arrange
        Grade grade = new Grade();
        grade.setOfferingId(OFFERING_ID);
        grade.setStudentId(STUDENT_ID);
        grade.setGrade("A");

        when(offeringRepository.findById(OFFERING_ID)).thenReturn(Optional.of(offering));
        when(enrollmentRepository.findByOfferingIdAndStatus(OFFERING_ID, EnrollmentStatus.ENROLLED))
                .thenReturn(List.of(enrollment));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(gradeRepository.findByOfferingIdAndStudentId(OFFERING_ID, STUDENT_ID))
                .thenReturn(Optional.of(grade));

        // Act
        List<StudentGradeDTO> result = gradeService.getRosterWithGrades(PROFESSOR_ID, OFFERING_ID);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(ENROLLMENT_ID, result.get(0).getEnrollmentId());
        assertEquals(STUDENT_ID, result.get(0).getStudentId());
        assertEquals("Charlie Brown", result.get(0).getStudentName());
        assertEquals("A", result.get(0).getGrade());
        assertEquals("ENROLLED", result.get(0).getStatus());
    }

    @Test
    @DisplayName("获取班级名单 - 开设不存在时抛出404异常")
    void getRosterWithGrades_offeringNotFound_throwsNotFoundException() {
        // Arrange
        when(offeringRepository.findById(OFFERING_ID)).thenReturn(Optional.empty());

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class,
                () -> gradeService.getRosterWithGrades(PROFESSOR_ID, OFFERING_ID));
        assertEquals(404, exception.getStatus());
        assertEquals("开设不存在", exception.getMessage());
    }

    @Test
    @DisplayName("获取班级名单 - 非任教教授访问时抛出403异常(水平越权防护)")
    void getRosterWithGrades_differentProfessor_throwsForbiddenException() {
        // Arrange
        when(offeringRepository.findById(OFFERING_ID)).thenReturn(Optional.of(offering));

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class,
                () -> gradeService.getRosterWithGrades(OTHER_PROFESSOR_ID, OFFERING_ID));
        assertEquals(403, exception.getStatus());
        assertEquals("只能查看自己任教的班级", exception.getMessage());
    }

    // ==================== submitGrade 测试 ====================

    @Test
    @DisplayName("录入成绩 - 有效成绩A → 成功并返回DTO")
    void submitGrade_validGradeA_succeeds() {
        // Arrange
        when(offeringRepository.findById(OFFERING_ID)).thenReturn(Optional.of(offering));
        when(enrollmentRepository.findByOfferingIdAndStatus(OFFERING_ID, EnrollmentStatus.ENROLLED))
                .thenReturn(List.of(enrollment));
        when(gradeRepository.findByOfferingIdAndStudentId(OFFERING_ID, STUDENT_ID))
                .thenReturn(Optional.empty());
        when(gradeRepository.save(any(Grade.class))).thenAnswer(invocation -> {
            Grade g = invocation.getArgument(0);
            g.setId(100L);
            return g;
        });
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));

        // Act
        StudentGradeDTO result = gradeService.submitGrade(PROFESSOR_ID, OFFERING_ID,
                STUDENT_ID, "A", false);

        // Assert
        assertNotNull(result);
        assertEquals(STUDENT_ID, result.getStudentId());
        assertEquals("Charlie Brown", result.getStudentName());
        assertEquals("A", result.getGrade());
        assertEquals("ENROLLED", result.getStatus());
        verify(gradeRepository).save(any(Grade.class));
        verify(auditService).recordGradeEntry(PROFESSOR_ID, OFFERING_ID, STUDENT_ID,
                COURSE_ID, "A");
        verify(notificationService).notifyGradePosted(STUDENT_ID, OFFERING_ID, COURSE_ID, "A");
    }

    @Test
    @DisplayName("录入成绩 - 无效成绩G → 抛出400异常")
    void submitGrade_invalidGradeG_throwsBadRequestException() {
        // Arrange
        when(offeringRepository.findById(OFFERING_ID)).thenReturn(Optional.of(offering));
        when(enrollmentRepository.findByOfferingIdAndStatus(OFFERING_ID, EnrollmentStatus.ENROLLED))
                .thenReturn(List.of(enrollment));

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class,
                () -> gradeService.submitGrade(PROFESSOR_ID, OFFERING_ID,
                        STUDENT_ID, "G", false));
        assertEquals(400, exception.getStatus());
        assertTrue(exception.getMessage().contains("成绩取值必须为 A/B/C/D/F/I"));
    }

    @Test
    @DisplayName("录入成绩 - 空字符串成绩且clear=false → 抛出400异常")
    void submitGrade_emptyGradeWithoutClear_throwsBadRequestException() {
        // Arrange
        when(offeringRepository.findById(OFFERING_ID)).thenReturn(Optional.of(offering));
        when(enrollmentRepository.findByOfferingIdAndStatus(OFFERING_ID, EnrollmentStatus.ENROLLED))
                .thenReturn(List.of(enrollment));

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class,
                () -> gradeService.submitGrade(PROFESSOR_ID, OFFERING_ID,
                        STUDENT_ID, "", false));
        assertEquals(400, exception.getStatus());
        assertTrue(exception.getMessage().contains("成绩不能为空"));
    }

    @Test
    @DisplayName("清除成绩 - clear=true 时成绩设为null")
    void submitGrade_clearGradeTrue_setsGradeToNull() {
        // Arrange
        when(offeringRepository.findById(OFFERING_ID)).thenReturn(Optional.of(offering));
        when(enrollmentRepository.findByOfferingIdAndStatus(OFFERING_ID, EnrollmentStatus.ENROLLED))
                .thenReturn(List.of(enrollment));
        when(gradeRepository.findByOfferingIdAndStudentId(OFFERING_ID, STUDENT_ID))
                .thenReturn(Optional.empty());
        when(gradeRepository.save(any(Grade.class))).thenAnswer(invocation -> {
            Grade g = invocation.getArgument(0);
            g.setId(100L);
            return g;
        });
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));

        // Act
        StudentGradeDTO result = gradeService.submitGrade(PROFESSOR_ID, OFFERING_ID,
                STUDENT_ID, null, true);

        // Assert
        assertNotNull(result);
        assertNull(result.getGrade());
        verify(gradeRepository).save(argThat(g -> g.getGrade() == null));
        verify(notificationService, never()).notifyGradePosted(anyLong(), anyLong(),
                anyString(), anyString());
    }

    @Test
    @DisplayName("录入成绩 - 非任教教授 → 抛出403异常(水平越权防护)")
    void submitGrade_differentProfessor_throwsForbiddenException() {
        // Arrange
        when(offeringRepository.findById(OFFERING_ID)).thenReturn(Optional.of(offering));

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class,
                () -> gradeService.submitGrade(OTHER_PROFESSOR_ID, OFFERING_ID,
                        STUDENT_ID, "A", false));
        assertEquals(403, exception.getStatus());
        assertEquals("只能为自己任教的班级录入成绩", exception.getMessage());
        verify(gradeRepository, never()).save(any());
    }

    @Test
    @DisplayName("录入成绩 - 更新已有成绩(重新评分)")
    void submitGrade_existingGrade_updatesGradeSuccessfully() {
        // Arrange
        Grade existingGrade = new Grade();
        existingGrade.setId(100L);
        existingGrade.setOfferingId(OFFERING_ID);
        existingGrade.setStudentId(STUDENT_ID);
        existingGrade.setGrade("B");

        when(offeringRepository.findById(OFFERING_ID)).thenReturn(Optional.of(offering));
        when(enrollmentRepository.findByOfferingIdAndStatus(OFFERING_ID, EnrollmentStatus.ENROLLED))
                .thenReturn(List.of(enrollment));
        when(gradeRepository.findByOfferingIdAndStudentId(OFFERING_ID, STUDENT_ID))
                .thenReturn(Optional.of(existingGrade));
        when(gradeRepository.save(any(Grade.class))).thenAnswer(invocation -> {
            Grade g = invocation.getArgument(0);
            return g;
        });
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));

        // Act
        StudentGradeDTO result = gradeService.submitGrade(PROFESSOR_ID, OFFERING_ID,
                STUDENT_ID, "A", false);

        // Assert
        assertNotNull(result);
        assertEquals("A", result.getGrade());
        // 验证更新了已有记录而非创建新记录
        verify(gradeRepository).save(argThat(g -> g.getId().equals(100L)));
        verify(auditService).recordGradeEntry(PROFESSOR_ID, OFFERING_ID, STUDENT_ID,
                COURSE_ID, "A");
    }

    @Test
    @DisplayName("录入成绩 - 学生未注册此课程 → 抛出400异常")
    void submitGrade_studentNotEnrolled_throwsBadRequestException() {
        // Arrange - offering exists and professor matches, but student not in enrollments
        when(offeringRepository.findById(OFFERING_ID)).thenReturn(Optional.of(offering));
        when(enrollmentRepository.findByOfferingIdAndStatus(OFFERING_ID, EnrollmentStatus.ENROLLED))
                .thenReturn(List.of());

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class,
                () -> gradeService.submitGrade(PROFESSOR_ID, OFFERING_ID,
                        STUDENT_ID, "A", false));
        assertEquals(400, exception.getStatus());
        assertEquals("学生未注册此课程", exception.getMessage());
    }

    @Test
    @DisplayName("录入成绩 - 成绩不区分大小写(小写a自动转为大写A)")
    void submitGrade_lowercaseGrade_convertsToUpper() {
        // Arrange
        when(offeringRepository.findById(OFFERING_ID)).thenReturn(Optional.of(offering));
        when(enrollmentRepository.findByOfferingIdAndStatus(OFFERING_ID, EnrollmentStatus.ENROLLED))
                .thenReturn(List.of(enrollment));
        when(gradeRepository.findByOfferingIdAndStudentId(OFFERING_ID, STUDENT_ID))
                .thenReturn(Optional.empty());
        when(gradeRepository.save(any(Grade.class))).thenAnswer(invocation -> {
            Grade g = invocation.getArgument(0);
            g.setId(100L);
            return g;
        });
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));

        // Act
        StudentGradeDTO result = gradeService.submitGrade(PROFESSOR_ID, OFFERING_ID,
                STUDENT_ID, "a", false);

        // Assert
        assertNotNull(result);
        assertEquals("A", result.getGrade());
        verify(auditService).recordGradeEntry(PROFESSOR_ID, OFFERING_ID, STUDENT_ID,
                COURSE_ID, "A");
    }
}
