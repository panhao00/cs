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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Leveling 算法集成测试。
 * <p>
 * 测试用例 7.8 Close Registration 的步骤 3: Leveling 课表均衡算法。
 * 验证:
 * <ul>
 *   <li>学生 DRAFT 课表中, 主选被取消后备选提升为主选</li>
 *   <li>遵守 maxPrimary (4 门) 上限</li>
 *   <li>已有足够主选的学生不受影响</li>
 *   <li>开设无容量时跳过提升</li>
 *   <li>开设非 OPEN 状态时跳过提升</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LevelingIntegrationTest {

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

    // ==================== 场景 1: 备选被提升为主选 ====================

    @Test
    @DisplayName("Leveling - 主选开设人数不足, 备选被提升为主选")
    void leveling_alternatePromotedWhenPrimaryUnderMin() {
        // ========== 场景 ==========
        // o1 (CS101): 有教授, 5人 >= 3 -> 步骤2 COMMITTED
        // o2 (CS201): 有教授, 2人 < 3 -> 步骤2 暂不处理, 步骤4 重新检查
        // 学生课表: 1个 PRIMARY (ENROLLED_IN) + 1个 ALTERNATE (SELECTED, 指向 o2)
        // Leveling: primaryEnrolled=1 < maxPrimary(4), 提升 o2 的备选

        // Given
        when(registrationPhase.getPhase()).thenReturn("open");

        CourseOffering o1 = createOffering(1L, "CS101", 1L, OfferingStatus.OPEN);
        CourseOffering o2 = createOffering(2L, "CS201", 2L, OfferingStatus.OPEN);

        // 步骤2: 第一次返回 [o1, o2]
        // 步骤4: 第二次返回 [o2] (o1 已 COMMITTED)
        // 步骤6: 第三次返回 [] (o2 leveling 后 COMMITTED)
        when(offeringRepository.findBySemesterAndStatus(SEMESTER, OfferingStatus.OPEN))
                .thenReturn(
                        new ArrayList<>(List.of(o1, o2)),
                        new ArrayList<>(List.of(o2)),
                        List.of());

        // 步骤2: o1 有 5 人, o2 有 2 人
        when(enrollmentRepository.findByOfferingIdAndStatus(1L, EnrollmentStatus.ENROLLED))
                .thenReturn(List.of(
                        createEnrollment(1L, 1L, 10L), createEnrollment(2L, 1L, 11L),
                        createEnrollment(3L, 1L, 12L), createEnrollment(4L, 1L, 13L),
                        createEnrollment(5L, 1L, 14L)));
        when(enrollmentRepository.findByOfferingIdAndStatus(2L, EnrollmentStatus.ENROLLED))
                .thenReturn(List.of(
                        createEnrollment(6L, 2L, 10L), createEnrollment(7L, 2L, 11L)));

        // 步骤3: 学生 10L 的课表
        Schedule sched = createSchedule(1L, 10L, SEMESTER, ScheduleState.DRAFT);
        when(scheduleRepository.findBySemesterAndState(SEMESTER, ScheduleState.DRAFT))
                .thenReturn(List.of(sched));

        // 课表项: 1个 PRIMARY (已注册) + 1个 ALTERNATE (待选)
        ScheduleItem item1 = createScheduleItem(1L, 1L, 1L,
                ItemKind.PRIMARY, ItemStatus.ENROLLED_IN);
        ScheduleItem item2 = createScheduleItem(2L, 1L, 2L,
                ItemKind.ALTERNATE, ItemStatus.SELECTED);
        when(itemRepository.findByScheduleId(1L)).thenReturn(List.of(item1, item2));

        // Leveling: o2 OPEN, 有容量 -> 提升 item2
        when(offeringRepository.findById(2L)).thenReturn(Optional.of(o2));
        when(enrollmentRepository.countByOfferingIdAndStatus(2L, EnrollmentStatus.ENROLLED))
                .thenReturn(2L, 3L); // leveling 时=2, step4/finalStatuses 时=3

        // 步骤4: o2 leveling 后 3人 >= 3 -> COMMITTED
        when(offeringRepository.findById(1L)).thenReturn(Optional.of(o1));
        when(enrollmentRepository.countByOfferingIdAndStatus(1L, EnrollmentStatus.ENROLLED))
                .thenReturn(5L);

        // 步骤6
        when(offeringRepository.findBySemesterAndStatus(SEMESTER, OfferingStatus.COMMITTED))
                .thenReturn(List.of());

        // When
        CloseRegistrationResult result = registrationService.closeRegistration(SEMESTER);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(SEMESTER, result.getSemester());
        // o1 已 COMMITTED
        assertEquals(OfferingStatus.COMMITTED, o1.getStatus());
        // o2 leveling 后也 COMMITTED
        assertEquals(OfferingStatus.COMMITTED, o2.getStatus());
        // item2 被提升为主选
        assertEquals(ItemKind.PRIMARY, item2.getKind());
        assertEquals(ItemStatus.ENROLLED_IN, item2.getStatus());
        // item1 不变
        assertEquals(ItemKind.PRIMARY, item1.getKind());
        assertEquals(ItemStatus.ENROLLED_IN, item1.getStatus());
        // Leveling 新增 1 条 Enrollment
        verify(enrollmentRepository, times(1)).save(any(Enrollment.class));
        // item2 保存 (提升)
        verify(itemRepository, times(1)).save(item2);
    }

    // ==================== 场景 2: 达到主选上限 ====================

    @Test
    @DisplayName("Leveling - 已有4个主选, 不再提升备选 (达到 maxPrimary 上限)")
    void leveling_noPromotionWhenMaxPrimaryReached() {
        // ========== 场景 ==========
        // o1: 有教授, 3人 -> COMMITTED
        // 学生课表: 4个 PRIMARY (ENROLLED_IN) + 1个 ALTERNATE (SELECTED)
        // primaryEnrolled=4 >= maxPrimary(4), 跳过 Leveling

        // Given
        when(registrationPhase.getPhase()).thenReturn("open");

        CourseOffering o1 = createOffering(1L, "CS101", 1L, OfferingStatus.OPEN);

        when(offeringRepository.findBySemesterAndStatus(SEMESTER, OfferingStatus.OPEN))
                .thenReturn(new ArrayList<>(List.of(o1)), List.of());
        when(enrollmentRepository.findByOfferingIdAndStatus(1L, EnrollmentStatus.ENROLLED))
                .thenReturn(List.of(
                        createEnrollment(1L, 1L, 10L), createEnrollment(2L, 1L, 11L),
                        createEnrollment(3L, 1L, 12L)));

        // 步骤3: 学生课表已有 4 个主选
        Schedule sched = createSchedule(1L, 10L, SEMESTER, ScheduleState.DRAFT);
        when(scheduleRepository.findBySemesterAndState(SEMESTER, ScheduleState.DRAFT))
                .thenReturn(List.of(sched));

        ScheduleItem item1 = createScheduleItem(1L, 1L, 1L,
                ItemKind.PRIMARY, ItemStatus.ENROLLED_IN);
        ScheduleItem item2 = createScheduleItem(2L, 1L, 2L,
                ItemKind.PRIMARY, ItemStatus.ENROLLED_IN);
        ScheduleItem item3 = createScheduleItem(3L, 1L, 3L,
                ItemKind.PRIMARY, ItemStatus.ENROLLED_IN);
        ScheduleItem item4 = createScheduleItem(4L, 1L, 4L,
                ItemKind.PRIMARY, ItemStatus.ENROLLED_IN);
        ScheduleItem item5 = createScheduleItem(5L, 1L, 5L,
                ItemKind.ALTERNATE, ItemStatus.SELECTED);
        when(itemRepository.findByScheduleId(1L))
                .thenReturn(List.of(item1, item2, item3, item4, item5));

        // finalStatuses
        when(offeringRepository.findById(1L)).thenReturn(Optional.of(o1));
        when(enrollmentRepository.countByOfferingIdAndStatus(1L, EnrollmentStatus.ENROLLED))
                .thenReturn(3L);

        // 步骤6
        when(offeringRepository.findBySemesterAndStatus(SEMESTER, OfferingStatus.COMMITTED))
                .thenReturn(List.of());

        // When
        CloseRegistrationResult result = registrationService.closeRegistration(SEMESTER);

        // Then
        assertTrue(result.isSuccess());
        // 备选 item5 未被提升
        assertEquals(ItemKind.ALTERNATE, item5.getKind());
        assertEquals(ItemStatus.SELECTED, item5.getStatus());
        // 未新增 Enrollment (maxPrimary 已满)
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
        // 未修改任何课表项
        verify(itemRepository, never()).save(any(ScheduleItem.class));
    }

    // ==================== 场景 3: 只有备选全部提升 ====================

    @Test
    @DisplayName("Leveling - 学生只有备选, 全部被提升为主选")
    void leveling_alternatesOnlyPromotedToPrimary() {
        // ========== 场景 ==========
        // o1: 有教授, 2人 < 3 -> 步骤2 暂不处理
        // o2: 有教授, 2人 < 3 -> 步骤2 暂不处理
        // 学生课表: 0个 PRIMARY + 2个 ALTERNATE (分别指向 o1, o2)
        // Leveling: primaryEnrolled=0, 两个备选都被提升

        // Given
        when(registrationPhase.getPhase()).thenReturn("open");

        CourseOffering o1 = createOffering(1L, "CS101", 1L, OfferingStatus.OPEN);
        CourseOffering o2 = createOffering(2L, "CS201", 2L, OfferingStatus.OPEN);

        // 步骤2: [o1, o2] 都暂不处理
        // 步骤4: [o1, o2] 重新检查
        // 步骤6: [] 已全部处理
        when(offeringRepository.findBySemesterAndStatus(SEMESTER, OfferingStatus.OPEN))
                .thenReturn(
                        new ArrayList<>(List.of(o1, o2)),
                        new ArrayList<>(List.of(o1, o2)),
                        List.of());

        when(enrollmentRepository.findByOfferingIdAndStatus(1L, EnrollmentStatus.ENROLLED))
                .thenReturn(List.of(createEnrollment(1L, 1L, 10L), createEnrollment(2L, 1L, 11L)));
        when(enrollmentRepository.findByOfferingIdAndStatus(2L, EnrollmentStatus.ENROLLED))
                .thenReturn(List.of(createEnrollment(3L, 2L, 10L), createEnrollment(4L, 2L, 11L)));

        // 步骤3: 学生只有备选
        Schedule sched = createSchedule(1L, 10L, SEMESTER, ScheduleState.DRAFT);
        when(scheduleRepository.findBySemesterAndState(SEMESTER, ScheduleState.DRAFT))
                .thenReturn(List.of(sched));

        ScheduleItem item1 = createScheduleItem(1L, 1L, 1L,
                ItemKind.ALTERNATE, ItemStatus.SELECTED);
        ScheduleItem item2 = createScheduleItem(2L, 1L, 2L,
                ItemKind.ALTERNATE, ItemStatus.SELECTED);
        when(itemRepository.findByScheduleId(1L)).thenReturn(List.of(item1, item2));

        // Leveling: o1 OPEN, 有容量 -> 提升 item1
        when(offeringRepository.findById(1L)).thenReturn(Optional.of(o1));
        when(enrollmentRepository.countByOfferingIdAndStatus(1L, EnrollmentStatus.ENROLLED))
                .thenReturn(2L, 3L); // leveling=2, step4=3

        // Leveling: o2 OPEN, 有容量 -> 提升 item2
        when(offeringRepository.findById(2L)).thenReturn(Optional.of(o2));
        when(enrollmentRepository.countByOfferingIdAndStatus(2L, EnrollmentStatus.ENROLLED))
                .thenReturn(2L, 3L); // leveling=2, step4=3

        // finalStatuses
        when(enrollmentRepository.countByOfferingIdAndStatus(1L, EnrollmentStatus.ENROLLED))
                .thenReturn(3L);
        when(enrollmentRepository.countByOfferingIdAndStatus(2L, EnrollmentStatus.ENROLLED))
                .thenReturn(3L);

        // 步骤6
        when(offeringRepository.findBySemesterAndStatus(SEMESTER, OfferingStatus.COMMITTED))
                .thenReturn(List.of());

        // When
        CloseRegistrationResult result = registrationService.closeRegistration(SEMESTER);

        // Then
        assertTrue(result.isSuccess());
        // 两个备选都被提升
        assertEquals(ItemKind.PRIMARY, item1.getKind());
        assertEquals(ItemStatus.ENROLLED_IN, item1.getStatus());
        assertEquals(ItemKind.PRIMARY, item2.getKind());
        assertEquals(ItemStatus.ENROLLED_IN, item2.getStatus());
        // 两个开设 leveling 后满足人数 -> COMMITTED
        assertEquals(OfferingStatus.COMMITTED, o1.getStatus());
        assertEquals(OfferingStatus.COMMITTED, o2.getStatus());
        // 新增 2 条 Enrollment
        verify(enrollmentRepository, times(2)).save(any(Enrollment.class));
    }

    // ==================== 场景 4: 备选开设已满 ====================

    @Test
    @DisplayName("Leveling - 备选开设已满时跳过提升")
    void leveling_alternateSkippedWhenOfferingFull() {
        // ========== 场景 ==========
        // o1: COMMITTED (3人)
        // o2: 有教授, 2人 < 3 -> 暂不处理, 但 Leveling 时 countBy 返回 10 (已满)
        // o3: 有教授, 2人 < 3 -> 暂不处理, Leveling 时有容量
        // 学生: 1 PRIMARY (o1) + 2 ALTERNATE (o2, o3)
        // Leveling: item(o2) 已满跳过, item(o3) 提升

        // Given
        when(registrationPhase.getPhase()).thenReturn("open");

        CourseOffering o1 = createOffering(1L, "CS101", 1L, OfferingStatus.OPEN);
        CourseOffering o2 = createOffering(2L, "CS201", 2L, OfferingStatus.OPEN);
        CourseOffering o3 = createOffering(3L, "MATH101", 1L, OfferingStatus.OPEN);

        when(offeringRepository.findBySemesterAndStatus(SEMESTER, OfferingStatus.OPEN))
                .thenReturn(
                        new ArrayList<>(List.of(o1, o2, o3)),
                        new ArrayList<>(List.of(o2, o3)),
                        List.of());

        when(enrollmentRepository.findByOfferingIdAndStatus(1L, EnrollmentStatus.ENROLLED))
                .thenReturn(List.of(
                        createEnrollment(1L, 1L, 10L), createEnrollment(2L, 1L, 11L),
                        createEnrollment(3L, 1L, 12L)));
        when(enrollmentRepository.findByOfferingIdAndStatus(2L, EnrollmentStatus.ENROLLED))
                .thenReturn(List.of(createEnrollment(4L, 2L, 10L), createEnrollment(5L, 2L, 11L)));
        when(enrollmentRepository.findByOfferingIdAndStatus(3L, EnrollmentStatus.ENROLLED))
                .thenReturn(List.of(createEnrollment(6L, 3L, 10L), createEnrollment(7L, 3L, 11L)));

        // 步骤3: 学生课表
        Schedule sched = createSchedule(1L, 10L, SEMESTER, ScheduleState.DRAFT);
        when(scheduleRepository.findBySemesterAndState(SEMESTER, ScheduleState.DRAFT))
                .thenReturn(List.of(sched));

        ScheduleItem item1 = createScheduleItem(1L, 1L, 1L,
                ItemKind.PRIMARY, ItemStatus.ENROLLED_IN);
        ScheduleItem item2 = createScheduleItem(2L, 1L, 2L,
                ItemKind.ALTERNATE, ItemStatus.SELECTED);
        ScheduleItem item3 = createScheduleItem(3L, 1L, 3L,
                ItemKind.ALTERNATE, ItemStatus.SELECTED);
        when(itemRepository.findByScheduleId(1L)).thenReturn(List.of(item1, item2, item3));

        // Leveling: o2 已满 (10人 = maxStudents) -> 跳过
        when(offeringRepository.findById(2L)).thenReturn(Optional.of(o2));
        when(enrollmentRepository.countByOfferingIdAndStatus(2L, EnrollmentStatus.ENROLLED))
                .thenReturn(10L);

        // Leveling: o3 有容量 -> 提升 item3
        when(offeringRepository.findById(3L)).thenReturn(Optional.of(o3));
        when(enrollmentRepository.countByOfferingIdAndStatus(3L, EnrollmentStatus.ENROLLED))
                .thenReturn(2L, 3L); // leveling=2, step4=3

        // finalStatuses
        when(offeringRepository.findById(1L)).thenReturn(Optional.of(o1));
        when(enrollmentRepository.countByOfferingIdAndStatus(1L, EnrollmentStatus.ENROLLED))
                .thenReturn(3L);

        // 步骤6
        when(offeringRepository.findBySemesterAndStatus(SEMESTER, OfferingStatus.COMMITTED))
                .thenReturn(List.of());

        // When
        CloseRegistrationResult result = registrationService.closeRegistration(SEMESTER);

        // Then
        assertTrue(result.isSuccess());
        // o2 已满, item2 未被提升
        assertEquals(ItemKind.ALTERNATE, item2.getKind());
        assertEquals(ItemStatus.SELECTED, item2.getStatus());
        // o3 有容量, item3 被提升
        assertEquals(ItemKind.PRIMARY, item3.getKind());
        assertEquals(ItemStatus.ENROLLED_IN, item3.getStatus());
        // 只新增 1 条 Enrollment (仅 item3)
        verify(enrollmentRepository, times(1)).save(any(Enrollment.class));
    }

    // ==================== 场景 5: 备选开设非 OPEN ====================

    @Test
    @DisplayName("Leveling - 备选开设已取消时跳过提升")
    void leveling_alternateSkippedWhenOfferingNotOpen() {
        // ========== 场景 ==========
        // o1: 有教授, 3人 -> COMMITTED
        // o2: 无教授 -> 步骤2 CANCELLED
        // o3: 有教授, 2人 < 3 -> 暂不处理
        // 学生: 1 PRIMARY (o1) + 1 PRIMARY (o2->CANCELLED) + 1 ALTERNATE (o3)
        // Leveling: item(o2) 的开设已 CANCELLED -> 跳过; item(o3) 提升

        // Given
        when(registrationPhase.getPhase()).thenReturn("open");

        CourseOffering o1 = createOffering(1L, "CS101", 1L, OfferingStatus.OPEN);
        CourseOffering o2 = createOffering(2L, "CS201", null, OfferingStatus.OPEN);
        CourseOffering o3 = createOffering(3L, "MATH101", 1L, OfferingStatus.OPEN);

        when(offeringRepository.findBySemesterAndStatus(SEMESTER, OfferingStatus.OPEN))
                .thenReturn(
                        new ArrayList<>(List.of(o1, o2, o3)),
                        new ArrayList<>(List.of(o3)),
                        List.of());

        when(enrollmentRepository.findByOfferingIdAndStatus(1L, EnrollmentStatus.ENROLLED))
                .thenReturn(List.of(
                        createEnrollment(1L, 1L, 10L), createEnrollment(2L, 1L, 11L),
                        createEnrollment(3L, 1L, 12L)));
        when(enrollmentRepository.findByOfferingIdAndStatus(2L, EnrollmentStatus.ENROLLED))
                .thenReturn(List.of(createEnrollment(4L, 2L, 10L)));
        when(enrollmentRepository.findByOfferingIdAndStatus(3L, EnrollmentStatus.ENROLLED))
                .thenReturn(List.of(createEnrollment(5L, 3L, 10L), createEnrollment(6L, 3L, 11L)));

        // 步骤2: o2 无教授 -> cancelAllEnrollments + removeFromAllSchedules
        when(offeringRepository.findById(2L)).thenReturn(Optional.of(o2));
        when(itemRepository.findByOfferingId(2L)).thenReturn(List.of());

        // 步骤3: 学生课表
        Schedule sched = createSchedule(1L, 10L, SEMESTER, ScheduleState.DRAFT);
        when(scheduleRepository.findBySemesterAndState(SEMESTER, ScheduleState.DRAFT))
                .thenReturn(List.of(sched));

        // item2 在步骤2 中被 removeFromAllSchedules 设为 CANCELLED
        ScheduleItem item1 = createScheduleItem(1L, 1L, 1L,
                ItemKind.PRIMARY, ItemStatus.ENROLLED_IN);
        ScheduleItem item2 = createScheduleItem(2L, 1L, 2L,
                ItemKind.PRIMARY, ItemStatus.CANCELLED);
        ScheduleItem item3 = createScheduleItem(3L, 1L, 3L,
                ItemKind.ALTERNATE, ItemStatus.SELECTED);
        when(itemRepository.findByScheduleId(1L)).thenReturn(List.of(item1, item2, item3));

        // Leveling: item3 -> o3 OPEN, 有容量 -> 提升
        when(offeringRepository.findById(3L)).thenReturn(Optional.of(o3));
        when(enrollmentRepository.countByOfferingIdAndStatus(3L, EnrollmentStatus.ENROLLED))
                .thenReturn(2L, 3L); // leveling=2, step4=3

        // finalStatuses
        when(offeringRepository.findById(1L)).thenReturn(Optional.of(o1));
        when(enrollmentRepository.countByOfferingIdAndStatus(1L, EnrollmentStatus.ENROLLED))
                .thenReturn(3L);

        // 步骤6
        when(offeringRepository.findBySemesterAndStatus(SEMESTER, OfferingStatus.COMMITTED))
                .thenReturn(List.of());

        // When
        CloseRegistrationResult result = registrationService.closeRegistration(SEMESTER);

        // Then
        assertTrue(result.isSuccess());
        // o2 被 CANCELLED (无教授)
        assertEquals(OfferingStatus.CANCELLED, o2.getStatus());
        // item2 保持 CANCELLED
        assertEquals(ItemStatus.CANCELLED, item2.getStatus());
        // item3 被提升 (o3 OPEN 且有容量)
        assertEquals(ItemKind.PRIMARY, item3.getKind());
        assertEquals(ItemStatus.ENROLLED_IN, item3.getStatus());
        // o2 取消时通知 1 次 (1个选课记录)
        verify(notificationService, times(1)).notifyCourseCancelled(2L);
        // save 被调用 2 次: o2 的选课记录设为 CANCELLED + Leveling 新增 item3 的 Enrollment
        verify(enrollmentRepository, times(2)).save(any(Enrollment.class));
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

    private Schedule createSchedule(Long id, Long studentId, String semester,
                                    ScheduleState state) {
        Schedule schedule = new Schedule();
        schedule.setId(id);
        schedule.setStudentId(studentId);
        schedule.setSemester(semester);
        schedule.setState(state);
        return schedule;
    }

    private ScheduleItem createScheduleItem(Long id, Long scheduleId, Long offeringId,
                                            ItemKind kind, ItemStatus status) {
        ScheduleItem item = new ScheduleItem();
        item.setId(id);
        item.setScheduleId(scheduleId);
        item.setOfferingId(offeringId);
        item.setKind(kind);
        item.setStatus(status);
        return item;
    }
}
