package com.crs.service;

import com.crs.common.ApiException;
import com.crs.common.RegistrationPhase;
import com.crs.domain.Course;
import com.crs.domain.CourseOffering;
import com.crs.domain.EnrollmentStatus;
import com.crs.domain.OfferingStatus;
import com.crs.repository.CourseOfferingRepository;
import com.crs.repository.CourseRepository;
import com.crs.repository.EnrollmentRepository;
import com.crs.web.dto.TeachingOfferingDTO;
import com.crs.web.dto.TeachingSelectionResult;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TeachingService 单元测试。
 * <p>
 * 测试用例 7.3 Select Courses to Teach:
 * - 时间冲突检测
 * - 授权与业务规则
 * - 课程选择操作
 */
@ExtendWith(MockitoExtension.class)
class TeachingServiceTest {

    @Mock
    private CourseOfferingRepository offeringRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private RegistrationPhase registrationPhase;

    @Mock
    private AuditService auditService;

    private TeachingService teachingService;

    private static final Long PROFESSOR_ID = 100L;
    private static final String SEMESTER = "2026-FALL";

    @BeforeEach
    void setUp() {
        // Manually construct to avoid Lombok @RequiredArgsConstructor + @InjectMocks incompatibility
        teachingService = new TeachingService(
                offeringRepository, courseRepository, enrollmentRepository,
                registrationPhase, auditService);
    }

    // ==================== 辅助方法 ====================

    private CourseOffering createOffering(Long id, String courseId, Long professorId,
                                          OfferingStatus status, String days, String times) {
        CourseOffering offering = new CourseOffering();
        offering.setId(id);
        offering.setCourseId(courseId);
        offering.setSemester(SEMESTER);
        offering.setProfessorId(professorId);
        offering.setStatus(status);
        offering.setDays(days);
        offering.setTimes(times);
        offering.setRoom("Room-" + id);
        offering.setMaxCapacity(10);
        return offering;
    }

    private Course createCourse(String courseId, String title, String department) {
        Course course = new Course();
        course.setCourseId(courseId);
        course.setTitle(title);
        course.setDepartment(department);
        return course;
    }

    private void mockCourseLookup(String courseId, String title, String department) {
        Course course = createCourse(courseId, title, department);
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    }

    private void mockEnrollmentCount(Long offeringId, long count) {
        when(enrollmentRepository.countByOfferingIdAndStatus(offeringId, EnrollmentStatus.ENROLLED))
                .thenReturn(count);
    }

    // ==================== getEligibleOfferings 测试 ====================

    @Test
    @DisplayName("获取有资格授课的开设 - 正常返回 OPEN 开设列表")
    void getEligibleOfferings_withOpenOfferings_returnsList() {
        // Arrange
        CourseOffering o1 = createOffering(1L, "CS101", null, OfferingStatus.OPEN, "MWF", "09:00-10:00");
        CourseOffering o2 = createOffering(2L, "CS201", null, OfferingStatus.OPEN, "TR", "14:00-15:00");

        when(offeringRepository.findBySemesterAndStatus(SEMESTER, OfferingStatus.OPEN))
                .thenReturn(List.of(o1, o2));
        mockCourseLookup("CS101", "Introduction to CS", "Computer Science");
        mockCourseLookup("CS201", "Data Structures", "Computer Science");
        mockEnrollmentCount(1L, 5);
        mockEnrollmentCount(2L, 3);

        // Act
        List<TeachingOfferingDTO> result = teachingService.getEligibleOfferings(PROFESSOR_ID, SEMESTER);

        // Assert
        assertEquals(2, result.size());
        assertEquals("CS101", result.get(0).getCourseId());
        assertEquals("CS201", result.get(1).getCourseId());
        assertFalse(result.get(0).isSelectedByMe());
        assertFalse(result.get(1).isSelectedByMe());
    }

    @Test
    @DisplayName("获取有资格授课的开设 - 无可用开设时返回空列表")
    void getEligibleOfferings_noOfferings_returnsEmptyList() {
        // Arrange
        when(offeringRepository.findBySemesterAndStatus(SEMESTER, OfferingStatus.OPEN))
                .thenReturn(List.of());

        // Act
        List<TeachingOfferingDTO> result = teachingService.getEligibleOfferings(PROFESSOR_ID, SEMESTER);

        // Assert
        assertTrue(result.isEmpty());
        verify(courseRepository, never()).findById(anyString());
    }

    @Test
    @DisplayName("获取有资格授课的开设 - 已选授课标记 selectedByMe=true")
    void getEligibleOfferings_withExistingProfessorAssignment_marksSelected() {
        // Arrange: o1 已分配给当前教授
        CourseOffering o1 = createOffering(1L, "CS101", PROFESSOR_ID, OfferingStatus.OPEN, "MWF", "09:00-10:00");
        CourseOffering o2 = createOffering(2L, "CS201", 999L, OfferingStatus.OPEN, "TR", "14:00-15:00");

        when(offeringRepository.findBySemesterAndStatus(SEMESTER, OfferingStatus.OPEN))
                .thenReturn(List.of(o1, o2));
        mockCourseLookup("CS101", "Introduction to CS", "Computer Science");
        mockCourseLookup("CS201", "Data Structures", "Computer Science");
        mockEnrollmentCount(1L, 0);
        mockEnrollmentCount(2L, 0);

        // Act
        List<TeachingOfferingDTO> result = teachingService.getEligibleOfferings(PROFESSOR_ID, SEMESTER);

        // Assert
        assertTrue(result.get(0).isSelectedByMe());
        assertFalse(result.get(1).isSelectedByMe());
    }

    // ==================== 时间冲突检测 ====================

    @Test
    @DisplayName("时间冲突 - 同一天同一时间段 → 返回冲突结果")
    void selectCoursesToTeach_withTimeConflict_returnsConflictResult() {
        // Arrange: 注册开放
        when(registrationPhase.getPhase()).thenReturn("open");
        // 两个 MWF 09:00-10:00 的开设 → 时间冲突
        CourseOffering o1 = createOffering(1L, "CS101", null, OfferingStatus.OPEN, "MWF", "09:00-10:00");
        CourseOffering o2 = createOffering(2L, "CS201", null, OfferingStatus.OPEN, "MWF", "09:00-10:00");

        when(offeringRepository.findBySemesterAndStatus(SEMESTER, OfferingStatus.OPEN))
                .thenReturn(List.of(o1, o2));
        mockCourseLookup("CS101", "Introduction to CS", "Computer Science");
        mockCourseLookup("CS201", "Data Structures", "Computer Science");
        mockEnrollmentCount(1L, 0);
        mockEnrollmentCount(2L, 0);

        // Act
        TeachingSelectionResult result = teachingService.selectCoursesToTeach(
                PROFESSOR_ID, SEMESTER, List.of(1L, 2L));

        // Assert
        assertFalse(result.isSuccess());
        assertFalse(result.getConflicts().isEmpty());
        assertEquals(1, result.getConflicts().size());

        // 验证冲突信息包含课程名
        TeachingSelectionResult.ConflictInfo conflict = result.getConflicts().get(0);
        assertEquals("CS101", conflict.getCourseId1());
        assertEquals("CS201", conflict.getCourseId2());
        assertTrue(conflict.getMessage().contains("CS101"));
        assertTrue(conflict.getMessage().contains("CS201"));

        // 冲突时不保存
        verify(offeringRepository, never()).save(any());
    }

    @Test
    @DisplayName("时间冲突 - 不同天的开设 → 无冲突,选择成功")
    void selectCoursesToTeach_differentDays_noConflict() {
        // Arrange: 注册开放
        when(registrationPhase.getPhase()).thenReturn("open");
        // MWF 和 TR 不同天 → 无冲突
        CourseOffering o1 = createOffering(1L, "CS101", null, OfferingStatus.OPEN, "MWF", "09:00-10:00");
        CourseOffering o2 = createOffering(2L, "CS201", null, OfferingStatus.OPEN, "TR", "09:00-10:00");

        when(offeringRepository.findBySemesterAndStatus(SEMESTER, OfferingStatus.OPEN))
                .thenReturn(List.of(o1, o2));
        mockCourseLookup("CS101", "Introduction to CS", "Computer Science");
        mockCourseLookup("CS201", "Data Structures", "Computer Science");
        mockEnrollmentCount(1L, 0);
        mockEnrollmentCount(2L, 0);

        // Act
        TeachingSelectionResult result = teachingService.selectCoursesToTeach(
                PROFESSOR_ID, SEMESTER, List.of(1L, 2L));

        // Assert
        assertTrue(result.isSuccess());
        assertTrue(result.getConflicts().isEmpty());
        assertEquals(2, result.getSelected().size());
        verify(offeringRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("时间冲突 - 同一天但不重叠时间段 → 无冲突")
    void selectCoursesToTeach_sameDayNonOverlapping_noConflict() {
        // Arrange: 注册开放
        when(registrationPhase.getPhase()).thenReturn("open");
        // 同是 MWF 但一个 08:00-09:00 另一个 09:00-10:00 → 边界不重叠
        CourseOffering o1 = createOffering(1L, "CS101", null, OfferingStatus.OPEN, "MWF", "08:00-09:00");
        CourseOffering o2 = createOffering(2L, "CS201", null, OfferingStatus.OPEN, "MWF", "09:00-10:00");

        when(offeringRepository.findBySemesterAndStatus(SEMESTER, OfferingStatus.OPEN))
                .thenReturn(List.of(o1, o2));
        mockCourseLookup("CS101", "Introduction to CS", "Computer Science");
        mockCourseLookup("CS201", "Data Structures", "Computer Science");
        mockEnrollmentCount(1L, 0);
        mockEnrollmentCount(2L, 0);

        // Act
        TeachingSelectionResult result = teachingService.selectCoursesToTeach(
                PROFESSOR_ID, SEMESTER, List.of(1L, 2L));

        // Assert: start1(480) < end2(600)=true, start2(540) < end1(540)=false → 无重叠
        assertTrue(result.isSuccess());
        assertTrue(result.getConflicts().isEmpty());
    }

    // ==================== 授权与业务规则 ====================

    @Test
    @DisplayName("注册关闭时选择课程 → 抛出 ApiException")
    void selectCoursesToTeach_registrationClosed_throwsException() {
        // Arrange
        when(registrationPhase.getPhase()).thenReturn("closed");

        // Act & Assert
        ApiException ex = assertThrows(ApiException.class, () ->
                teachingService.selectCoursesToTeach(PROFESSOR_ID, SEMESTER, List.of(1L)));
        assertEquals(409, ex.getStatus());
        assertTrue(ex.getMessage().contains("注册已关闭"));
    }

    @Test
    @DisplayName("选择不存在的开设 ID → 抛出 ApiException")
    void selectCoursesToTeach_nonExistentOffering_throwsException() {
        // Arrange: 注册开放
        when(registrationPhase.getPhase()).thenReturn("open");
        CourseOffering o1 = createOffering(1L, "CS101", null, OfferingStatus.OPEN, "MWF", "09:00-10:00");
        when(offeringRepository.findBySemesterAndStatus(SEMESTER, OfferingStatus.OPEN))
                .thenReturn(List.of(o1));

        // Act & Assert: 选择 ID=999 不存在
        ApiException ex = assertThrows(ApiException.class, () ->
                teachingService.selectCoursesToTeach(PROFESSOR_ID, SEMESTER, List.of(999L)));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("999"));
    }

    @Test
    @DisplayName("选择非 OPEN 状态的开设 → 抛出 ApiException")
    void selectCoursesToTeach_notOpenStatus_throwsException() {
        // Arrange: 注册开放
        when(registrationPhase.getPhase()).thenReturn("open");
        // 只有 COMMITTED 开设可用,目标 ID 不在 OPEN 列表中
        CourseOffering o1 = createOffering(1L, "CS101", PROFESSOR_ID, OfferingStatus.COMMITTED, "MWF", "09:00-10:00");
        when(offeringRepository.findBySemesterAndStatus(SEMESTER, OfferingStatus.OPEN))
                .thenReturn(List.of()); // 无 OPEN 开设

        // Act & Assert: 尝试选择 ID=1,但不在 OPEN 列表
        ApiException ex = assertThrows(ApiException.class, () ->
                teachingService.selectCoursesToTeach(PROFESSOR_ID, SEMESTER, List.of(1L)));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("不存在或未开放"));
    }

    // ==================== 课程选择操作 ====================

    @Test
    @DisplayName("空列表选择 - 清除所有已有授课指派")
    void selectCoursesToTeach_emptyList_deselectsAll() {
        // Arrange: 注册开放
        when(registrationPhase.getPhase()).thenReturn("open");
        // 教授已选 1L,现在提交空列表
        CourseOffering o1 = createOffering(1L, "CS101", PROFESSOR_ID, OfferingStatus.OPEN, "MWF", "09:00-10:00");

        when(offeringRepository.findBySemesterAndStatus(SEMESTER, OfferingStatus.OPEN))
                .thenReturn(List.of(o1));

        // Act
        TeachingSelectionResult result = teachingService.selectCoursesToTeach(
                PROFESSOR_ID, SEMESTER, List.of());

        // Assert
        assertTrue(result.isSuccess());
        assertNull(o1.getProfessorId()); // 教授已被移除
        verify(offeringRepository).save(o1);
        verify(auditService).recordTeachingSelection(PROFESSOR_ID, SEMESTER, 0, true);
    }

    @Test
    @DisplayName("选择新课程 - 正确指派教授并保存")
    void selectCoursesToTeach_selectNewOfferings_assignsProfessor() {
        // Arrange: 注册开放
        when(registrationPhase.getPhase()).thenReturn("open");
        // 教授选择新的开设 1L
        CourseOffering o1 = createOffering(1L, "CS101", null, OfferingStatus.OPEN, "MWF", "09:00-10:00");
        when(offeringRepository.findBySemesterAndStatus(SEMESTER, OfferingStatus.OPEN))
                .thenReturn(List.of(o1));
        mockCourseLookup("CS101", "Introduction to CS", "Computer Science");
        mockEnrollmentCount(1L, 0);

        // Act
        TeachingSelectionResult result = teachingService.selectCoursesToTeach(
                PROFESSOR_ID, SEMESTER, List.of(1L));

        // Assert
        assertTrue(result.isSuccess());
        assertEquals(PROFESSOR_ID, o1.getProfessorId()); // 教授已被指派
        verify(offeringRepository).save(o1);
        assertEquals(1, result.getSelected().size());
        assertTrue(result.getSelected().get(0).isSelectedByMe());
    }

    @Test
    @DisplayName("更新选课 - 旧开设移除教授,新开设指派教授")
    void selectCoursesToTeach_changeSelection_removesOldAssignsNew() {
        // Arrange: 注册开放
        when(registrationPhase.getPhase()).thenReturn("open");
        // 教授原来选 1L,现在改为选 2L
        CourseOffering o1 = createOffering(1L, "CS101", PROFESSOR_ID, OfferingStatus.OPEN, "MWF", "09:00-10:00");
        CourseOffering o2 = createOffering(2L, "CS201", null, OfferingStatus.OPEN, "TR", "14:00-15:00");

        when(offeringRepository.findBySemesterAndStatus(SEMESTER, OfferingStatus.OPEN))
                .thenReturn(List.of(o1, o2));
        mockCourseLookup("CS201", "Data Structures", "Computer Science");
        mockEnrollmentCount(2L, 0);

        // Act
        TeachingSelectionResult result = teachingService.selectCoursesToTeach(
                PROFESSOR_ID, SEMESTER, List.of(2L));

        // Assert
        assertTrue(result.isSuccess());
        assertNull(o1.getProfessorId()); // 1L 的教授被移除
        assertEquals(PROFESSOR_ID, o2.getProfessorId()); // 2L 获得教授
        verify(offeringRepository, times(2)).save(any());
        assertEquals(1, result.getSelected().size());
        assertEquals(2L, result.getSelected().get(0).getOfferingId());
    }
}
