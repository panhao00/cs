package com.crs.service;

import com.crs.common.RegistrationPhase;
import com.crs.domain.CourseOffering;
import com.crs.domain.Enrollment;
import com.crs.domain.EnrollmentStatus;
import com.crs.domain.ItemKind;
import com.crs.domain.ItemStatus;
import com.crs.domain.OfferingStatus;
import com.crs.domain.Schedule;
import com.crs.domain.ScheduleItem;
import com.crs.domain.ScheduleState;
import com.crs.repository.CourseOfferingRepository;
import com.crs.repository.EnrollmentRepository;
import com.crs.repository.ScheduleItemRepository;
import com.crs.repository.ScheduleRepository;
import com.crs.web.dto.CloseRegistrationResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RegistrationService 单元测试。
 * <p>
 * 测试用例 7.8 Close Registration 的核心算法:
 * - 步骤 2: 检查教授指派 + 最小人数
 * - 步骤 3: Leveling 算法
 * - 步骤 4: 二次检查 + 取消
 */
@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private CourseOfferingRepository offeringRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ScheduleItemRepository itemRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private RegistrationPhase registrationPhase;

    @InjectMocks
    private RegistrationService registrationService;

    private static final String SEMESTER = "2026-FALL";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(registrationService, "minStudents", 3);
        ReflectionTestUtils.setField(registrationService, "maxStudents", 10);
        ReflectionTestUtils.setField(registrationService, "maxPrimary", 4);
    }

    @Test
    @DisplayName("关闭注册 - 已关闭时抛出异常")
    void closeRegistration_whenAlreadyClosed_throwsException() {
        when(registrationPhase.getPhase()).thenReturn("closed");

        assertThrows(Exception.class, () ->
                registrationService.closeRegistration(SEMESTER));

        verify(registrationPhase, never()).setPhase(anyString());
    }

    @Test
    @DisplayName("关闭注册 - 有教授且满足最小人数 → COMMITTED")
    void closeRegistration_withProfessorAndMinStudents_commits() {
        // Given: 注册开放
        when(registrationPhase.getPhase()).thenReturn("open");

        // CS101 有教授, 3人 >= minStudents(3)
        CourseOffering offering = createOffering(1L, "CS101", 1L, OfferingStatus.OPEN);
        List<Enrollment> enrollments = List.of(
                createEnrollment(1L, 1L, 1L),
                createEnrollment(2L, 1L, 2L),
                createEnrollment(3L, 1L, 3L));

        // 步骤2 + 步骤4: 第一次返回 offering(OPEN), 第二次返回空(已被 COMMITTED)
        when(offeringRepository.findBySemesterAndStatus(SEMESTER, OfferingStatus.OPEN))
                .thenReturn(new ArrayList<>(List.of(offering)), List.of());
        when(enrollmentRepository.findByOfferingIdAndStatus(1L, EnrollmentStatus.ENROLLED))
                .thenReturn(enrollments);

        // 步骤3: Leveling — 空课表
        when(scheduleRepository.findBySemesterAndState(SEMESTER, ScheduleState.DRAFT))
                .thenReturn(List.of());

        // finalStatuses 块: findById 返回已 COMMITTED 的开设
        when(offeringRepository.findById(1L)).thenReturn(Optional.of(offering));
        when(enrollmentRepository.countByOfferingIdAndStatus(1L, EnrollmentStatus.ENROLLED))
                .thenReturn(3L);

        // 步骤6: notifyAllStudentsRegistrationClosed 查询 COMMITTED 开设
        when(offeringRepository.findBySemesterAndStatus(SEMESTER, OfferingStatus.COMMITTED))
                .thenReturn(List.of());

        // When
        CloseRegistrationResult result = registrationService.closeRegistration(SEMESTER);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(SEMESTER, result.getSemester());
        assertEquals(1, result.getTotalOfferings());
        // 验证开设被 COMMITTED
        assertEquals(OfferingStatus.COMMITTED, offering.getStatus());
        // 验证注册阶段被设为 closed
        verify(registrationPhase).setPhase("closed");
    }

    @Test
    @DisplayName("关闭注册 - 无教授 → CANCELLED 并通知学生")
    void closeRegistration_withoutProfessor_cancels() {
        // Given: 注册开放
        when(registrationPhase.getPhase()).thenReturn("open");

        // CS101 无教授
        CourseOffering offering = createOffering(1L, "CS101", null, OfferingStatus.OPEN);
        List<Enrollment> enrollments = List.of(createEnrollment(1L, 1L, 1L));

        // 步骤2 + 步骤4: 第一次返回 offering(OPEN), 第二次返回空(已被 CANCELLED)
        when(offeringRepository.findBySemesterAndStatus(SEMESTER, OfferingStatus.OPEN))
                .thenReturn(new ArrayList<>(List.of(offering)), List.of());
        when(enrollmentRepository.findByOfferingIdAndStatus(1L, EnrollmentStatus.ENROLLED))
                .thenReturn(enrollments);

        // cancelAllEnrollments 需要 findById
        when(offeringRepository.findById(1L)).thenReturn(Optional.of(offering));

        // removeFromAllSchedules — 空课表项
        when(itemRepository.findByOfferingId(1L)).thenReturn(List.of());

        // 步骤3: Leveling — 空课表
        when(scheduleRepository.findBySemesterAndState(SEMESTER, ScheduleState.DRAFT))
                .thenReturn(List.of());

        // finalStatuses: countBy 返回 0 (已取消)
        when(enrollmentRepository.countByOfferingIdAndStatus(1L, EnrollmentStatus.ENROLLED))
                .thenReturn(0L);

        // 步骤6: notifyAllStudentsRegistrationClosed 查询 COMMITTED 开设
        when(offeringRepository.findBySemesterAndStatus(SEMESTER, OfferingStatus.COMMITTED))
                .thenReturn(List.of());

        // When
        CloseRegistrationResult result = registrationService.closeRegistration(SEMESTER);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(0, result.getFullOfferings());
        assertEquals(1, result.getUnderFilledOfferings());
        // 验证开设被 CANCELLED
        assertEquals(OfferingStatus.CANCELLED, offering.getStatus());
        // 验证通知
        verify(notificationService, times(1)).notifyCourseCancelled(1L);
    }

    @Test
    @DisplayName("关闭注册 - 有教授但人数不足 → CANCELLED")
    void closeRegistration_underMinStudents_cancels() {
        // Given: 注册开放
        when(registrationPhase.getPhase()).thenReturn("open");

        // CS101 有教授, 2人 < minStudents(3)
        CourseOffering offering = createOffering(1L, "CS101", 1L, OfferingStatus.OPEN);
        List<Enrollment> enrollments = List.of(
                createEnrollment(1L, 1L, 1L),
                createEnrollment(2L, 1L, 2L));

        // 步骤2: offering 人数不足暂不处理
        when(offeringRepository.findBySemesterAndStatus(SEMESTER, OfferingStatus.OPEN))
                .thenReturn(new ArrayList<>(List.of(offering)));
        when(enrollmentRepository.findByOfferingIdAndStatus(1L, EnrollmentStatus.ENROLLED))
                .thenReturn(enrollments);

        // 步骤3: Leveling — 空课表
        when(scheduleRepository.findBySemesterAndState(SEMESTER, ScheduleState.DRAFT))
                .thenReturn(List.of());

        // 步骤4: 二次检查仍为 OPEN (未被 commit), 重新查询得到同一个 offering
        when(offeringRepository.findBySemesterAndStatus(SEMESTER, OfferingStatus.OPEN))
                .thenReturn(new ArrayList<>(List.of(offering)));
        when(enrollmentRepository.countByOfferingIdAndStatus(1L, EnrollmentStatus.ENROLLED))
                .thenReturn(2L);

        // cancelAllEnrollments + removeFromAllSchedules
        when(offeringRepository.findById(1L)).thenReturn(Optional.of(offering));
        when(itemRepository.findByOfferingId(1L)).thenReturn(List.of());

        // finalStatuses: countBy 返回 0 (已取消)
        when(enrollmentRepository.countByOfferingIdAndStatus(1L, EnrollmentStatus.ENROLLED))
                .thenReturn(0L);

        // 步骤6: notifyAllStudentsRegistrationClosed 查询 COMMITTED 开设
        when(offeringRepository.findBySemesterAndStatus(SEMESTER, OfferingStatus.COMMITTED))
                .thenReturn(List.of());

        // When
        CloseRegistrationResult result = registrationService.closeRegistration(SEMESTER);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(1, result.getTotalOfferings());
        assertEquals(0, result.getFullOfferings());
        assertEquals(1, result.getUnderFilledOfferings());
        // 验证开设被 CANCELLED
        assertEquals(OfferingStatus.CANCELLED, offering.getStatus());
    }

    @Test
    @DisplayName("预览关闭注册 - 统计正确")
    void previewCloseRegistration_statsCorrect() {
        // Given
        CourseOffering offering1 = createOffering(1L, "CS101", 1L, OfferingStatus.OPEN);
        CourseOffering offering2 = createOffering(2L, "CS201", null, OfferingStatus.OPEN);
        when(offeringRepository.findBySemesterAndStatus(SEMESTER, OfferingStatus.OPEN))
                .thenReturn(List.of(offering1, offering2));

        when(enrollmentRepository.countByOfferingIdAndStatus(1L, EnrollmentStatus.ENROLLED))
                .thenReturn(5L);
        when(enrollmentRepository.countByOfferingIdAndStatus(2L, EnrollmentStatus.ENROLLED))
                .thenReturn(2L);

        // When
        CloseRegistrationResult result = registrationService.previewCloseRegistration(SEMESTER);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(2, result.getTotalOfferings());
        // CS101: 有教授 + 5人 >= 3 → 满足
        // CS201: 无教授 → 不满足
        assertEquals(1, result.getFullOfferings());
        assertEquals(1, result.getUnderFilledOfferings());
    }

    // ==================== 辅助方法 ====================

    private CourseOffering createOffering(Long id, String courseId, Long professorId,
                                          OfferingStatus status) {
        CourseOffering offering = new CourseOffering();
        offering.setId(id);
        offering.setCourseId(courseId);
        offering.setSemester(SEMESTER);
        offering.setProfessorId(professorId);
        offering.setStatus(status);
        offering.setMaxCapacity(10);
        return offering;
    }

    private Enrollment createEnrollment(Long id, Long offeringId, Long studentId) {
        Enrollment enrollment = new Enrollment();
        enrollment.setId(id);
        enrollment.setOfferingId(offeringId);
        enrollment.setStudentId(studentId);
        enrollment.setStatus(EnrollmentStatus.ENROLLED);
        return enrollment;
    }
}
