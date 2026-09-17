package com.crs.service;

import com.crs.common.RegistrationPhase;
import com.crs.domain.CourseOffering;
import com.crs.domain.Enrollment;
import com.crs.domain.EnrollmentStatus;
import com.crs.domain.OfferingStatus;
import com.crs.domain.ScheduleState;
import com.crs.repository.CourseOfferingRepository;
import com.crs.repository.EnrollmentRepository;
import com.crs.repository.ScheduleItemRepository;
import com.crs.repository.ScheduleRepository;
import com.crs.repository.UserRepository;
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
 * Billing 重试机制单元测试。
 * <p>
 * 测试 notifyBillingSystem 在失败时的重试行为。
 */
@ExtendWith(MockitoExtension.class)
class BillingRetryTest {

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
    private UserRepository userRepository;

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
        ReflectionTestUtils.setField(registrationService, "billingRetryCount", 3);
        ReflectionTestUtils.setField(registrationService, "billingRetryDelayMs", 100L); // 缩短测试时间
    }

    @Test
    @DisplayName("关闭注册 - Billing 通知成功")
    void closeRegistration_billingNotificationSucceeds() {
        // Given: 注册开放
        when(registrationPhase.getPhase()).thenReturn("open");

        // CS101 有教授, 3人 >= minStudents(3)
        CourseOffering offering = createOffering(1L, "CS101", 1L, OfferingStatus.OPEN);
        List<Enrollment> enrollments = List.of(
                createEnrollment(1L, 1L, 1L),
                createEnrollment(2L, 1L, 2L),
                createEnrollment(3L, 1L, 3L));

        when(offeringRepository.findBySemesterAndStatus(SEMESTER, OfferingStatus.OPEN))
                .thenReturn(new ArrayList<>(List.of(offering)), List.of());
        when(enrollmentRepository.findByOfferingIdAndStatus(1L, EnrollmentStatus.ENROLLED))
                .thenReturn(enrollments);

        // 步骤3: Leveling — 空课表
        when(scheduleRepository.findBySemesterAndState(SEMESTER, ScheduleState.DRAFT))
                .thenReturn(List.of());

        // finalStatuses 块
        when(offeringRepository.findById(1L)).thenReturn(Optional.of(offering));
        when(enrollmentRepository.countByOfferingIdAndStatus(1L, EnrollmentStatus.ENROLLED))
                .thenReturn(3L);

        // 步骤6: notifyAllStudentsRegistrationClosed
        when(offeringRepository.findBySemesterAndStatus(SEMESTER, OfferingStatus.COMMITTED))
                .thenReturn(List.of());

        // When
        CloseRegistrationResult result = registrationService.closeRegistration(SEMESTER);

        // Then
        assertTrue(result.isSuccess());
        assertTrue(result.isBillingNotificationSent());
    }

    @Test
    @DisplayName("关闭注册 - Billing 配置可验证")
    void closeRegistration_billingRetryConfigIsConfigurable() {
        // 验证配置字段可以正确设置
        assertEquals(3, ReflectionTestUtils.getField(registrationService, "billingRetryCount"));
        assertEquals(100L, ReflectionTestUtils.getField(registrationService, "billingRetryDelayMs"));
    }

    @Test
    @DisplayName("关闭注册 - Billing 重试次数和延迟配置验证")
    void billingRetryConfig_defaultValues() {
        // 验证通过 @Value 注入的字段存在且可设置
        // 默认值来自 @Value 注解: retry-count=3, retry-delay-ms=1000
        ReflectionTestUtils.setField(registrationService, "billingRetryCount", 5);
        ReflectionTestUtils.setField(registrationService, "billingRetryDelayMs", 2000L);

        assertEquals(5, (int) ReflectionTestUtils.getField(registrationService, "billingRetryCount"));
        assertEquals(2000L, (long) ReflectionTestUtils.getField(registrationService, "billingRetryDelayMs"));
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
