package com.crs.service;

import com.crs.common.ApiException;
import com.crs.common.RegistrationPhase;
import com.crs.domain.Course;
import com.crs.domain.CourseOffering;
import com.crs.domain.Enrollment;
import com.crs.domain.EnrollmentStatus;
import com.crs.domain.Grade;
import com.crs.domain.ItemKind;
import com.crs.domain.ItemStatus;
import com.crs.domain.OfferingStatus;
import com.crs.domain.Schedule;
import com.crs.domain.ScheduleItem;
import com.crs.domain.ScheduleState;
import com.crs.repository.CourseOfferingRepository;
import com.crs.repository.CourseRepository;
import com.crs.repository.EnrollmentRepository;
import com.crs.repository.GradeRepository;
import com.crs.repository.ScheduleItemRepository;
import com.crs.repository.ScheduleRepository;
import com.crs.web.dto.ScheduleResponse;
import com.crs.web.dto.SelectOfferingsRequest;
import com.crs.web.dto.SubmitResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
 * ScheduleService 单元测试。
 * <p>
 * 覆盖课表服务的五个核心功能域:
 * 1. 先修课校验 (Prerequisite Validation)
 * 2. 时间冲突检测 (Time Conflict Detection)
 * 3. 容量控制 (Capacity Control)
 * 4. 课表操作 (Schedule Operations)
 * 5. 业务规则 (Business Rules)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScheduleServiceTest {

    // ==================== Mock Dependencies ====================

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ScheduleItemRepository itemRepository;

    @Mock
    private CourseOfferingRepository offeringRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CatalogService catalogService;

    @Mock
    private GradeRepository gradeRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private RegistrationPhase registrationPhase;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ScheduleService scheduleService;

    // ==================== Constants ====================

    private static final Long STUDENT_ID = 1L;
    private static final String SEMESTER = "2026-FALL";

    // ==================== Setup ====================

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduleService, "maxPrimary", 4);
        ReflectionTestUtils.setField(scheduleService, "maxAlternate", 2);
        ReflectionTestUtils.setField(scheduleService, "offeringMaxStudents", 10);
        ReflectionTestUtils.setField(scheduleService, "offeringMinStudents", 3);

        // 默认注册开放 — service 现在使用 isOpen()/isClosed() 便捷方法
        lenient().when(registrationPhase.isOpen()).thenReturn(true);
        lenient().when(registrationPhase.isAddDrop()).thenReturn(false);
        lenient().when(registrationPhase.isClosed()).thenReturn(false);
    }

    // ==================== Test Categories ====================

    // ──────────────────────────────────────────────
    // 1. PREREQUISITE VALIDATION
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("1. 先修课校验")
    class PrerequisiteTests {

        @Test
        @DisplayName("提交时先修课未通过 → 报错 PREREQUISITE_NOT_MET")
        void submitSchedule_prerequisiteNotMet_returnsError() {
            // Arrange
            Schedule schedule = createSchedule(1L, ScheduleState.DRAFT);
            CourseOffering offering = createOffering(1L, "CS201");
            Course course = createCourse("CS201", "Data Structures", "CS101");

            when(scheduleRepository.findByStudentIdAndSemester(STUDENT_ID, SEMESTER))
                    .thenReturn(Optional.of(schedule));
            when(itemRepository.findByScheduleId(1L)).thenReturn(List.of(
                    createScheduleItem(1L, 1L, ItemKind.PRIMARY, ItemStatus.SELECTED)));
            when(offeringRepository.findById(1L)).thenReturn(Optional.of(offering));
            when(courseRepository.findById("CS201")).thenReturn(Optional.of(course));

            // 学生没有 CS101 的及格成绩
            when(gradeRepository.findByStudentId(STUDENT_ID)).thenReturn(List.of());

            // Act
            SubmitResult result = scheduleService.submitSchedule(STUDENT_ID, SEMESTER);

            // Assert
            assertFalse(result.isSuccess());
            assertTrue(result.getErrors().stream()
                    .anyMatch(e -> "PREREQUISITE_NOT_MET".equals(e.getErrorType())));
        }

        @Test
        @DisplayName("提交时已通过先修课 → 成功注册")
        void submitSchedule_prerequisiteMet_enrolls() {
            // Arrange
            Schedule schedule = createSchedule(1L, ScheduleState.DRAFT);
            CourseOffering offering = createOffering(1L, "CS201");
            Course course = createCourse("CS201", "Data Structures", "CS101");

            when(scheduleRepository.findByStudentIdAndSemester(STUDENT_ID, SEMESTER))
                    .thenReturn(Optional.of(schedule));
            when(itemRepository.findByScheduleId(1L)).thenReturn(List.of(
                    createScheduleItem(1L, 1L, ItemKind.PRIMARY, ItemStatus.SELECTED)));
            when(offeringRepository.findById(1L)).thenReturn(Optional.of(offering));
            when(courseRepository.findById("CS201")).thenReturn(Optional.of(course));

            // 学生有 CS101 的 B 成绩
            CourseOffering prereqOffering = createOffering(10L, "CS101");
            Grade grade = createGrade(1L, 10L, STUDENT_ID, "B");
            when(gradeRepository.findByStudentId(STUDENT_ID)).thenReturn(List.of(grade));
            when(offeringRepository.findById(10L)).thenReturn(Optional.of(prereqOffering));

            // 容量校验
            when(enrollmentRepository.countByOfferingIdAndStatus(1L, EnrollmentStatus.ENROLLED))
                    .thenReturn(5L);

            // Act
            SubmitResult result = scheduleService.submitSchedule(STUDENT_ID, SEMESTER);

            // Assert
            assertTrue(result.isSuccess());
            assertTrue(result.getErrors().isEmpty());
            assertEquals(1, result.getResults().size());
            verify(enrollmentRepository).save(any(Enrollment.class));
        }

        @Test
        @DisplayName("提交时先修课成绩为 F → 不算通过")
        void submitSchedule_prerequisiteFailedGrade_returnsError() {
            // Arrange
            Schedule schedule = createSchedule(1L, ScheduleState.DRAFT);
            CourseOffering offering = createOffering(1L, "CS201");
            Course course = createCourse("CS201", "Data Structures", "CS101");

            when(scheduleRepository.findByStudentIdAndSemester(STUDENT_ID, SEMESTER))
                    .thenReturn(Optional.of(schedule));
            when(itemRepository.findByScheduleId(1L)).thenReturn(List.of(
                    createScheduleItem(1L, 1L, ItemKind.PRIMARY, ItemStatus.SELECTED)));
            when(offeringRepository.findById(1L)).thenReturn(Optional.of(offering));
            when(courseRepository.findById("CS201")).thenReturn(Optional.of(course));

            // 学生有 CS101 的 F 成绩 — 不算通过
            CourseOffering prereqOffering = createOffering(10L, "CS101");
            Grade grade = createGrade(1L, 10L, STUDENT_ID, "F");
            when(gradeRepository.findByStudentId(STUDENT_ID)).thenReturn(List.of(grade));
            when(offeringRepository.findById(10L)).thenReturn(Optional.of(prereqOffering));

            // Act
            SubmitResult result = scheduleService.submitSchedule(STUDENT_ID, SEMESTER);

            // Assert
            assertFalse(result.isSuccess());
            assertTrue(result.getErrors().stream()
                    .anyMatch(e -> "PREREQUISITE_NOT_MET".equals(e.getErrorType())));
        }
    }

    // ──────────────────────────────────────────────
    // 2. TIME CONFLICT DETECTION
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("2. 时间冲突检测")
    class TimeConflictTests {

        @Test
        @DisplayName("同日同时段的两门课 → TIME_CONFLICT")
        void submitSchedule_overlappingDaysAndTimes_returnsConflict() {
            // Arrange — 两门课都在 MWF 09:00-10:00
            Schedule schedule = createSchedule(1L, ScheduleState.DRAFT);
            CourseOffering o1 = createOffering(1L, "CS101");
            o1.setDays("MWF");
            o1.setTimes("09:00-10:00");
            CourseOffering o2 = createOffering(2L, "CS201");
            o2.setDays("MWF");
            o2.setTimes("09:00-10:00");

            when(scheduleRepository.findByStudentIdAndSemester(STUDENT_ID, SEMESTER))
                    .thenReturn(Optional.of(schedule));
            when(itemRepository.findByScheduleId(1L)).thenReturn(List.of(
                    createScheduleItem(1L, 1L, ItemKind.PRIMARY, ItemStatus.SELECTED),
                    createScheduleItem(2L, 2L, ItemKind.PRIMARY, ItemStatus.SELECTED)));
            when(offeringRepository.findById(1L)).thenReturn(Optional.of(o1));
            when(offeringRepository.findById(2L)).thenReturn(Optional.of(o2));
            when(courseRepository.findById("CS101")).thenReturn(Optional.of(createCourse("CS101", "Intro CS")));
            when(courseRepository.findById("CS201")).thenReturn(Optional.of(createCourse("CS201", "Data Structures")));
            when(enrollmentRepository.countByOfferingIdAndStatus(anyLong(), any())).thenReturn(0L);

            // Act
            SubmitResult result = scheduleService.submitSchedule(STUDENT_ID, SEMESTER);

            // Assert — 两门课互相冲突
            assertFalse(result.isSuccess());
            assertTrue(result.getErrors().stream()
                    .anyMatch(e -> "TIME_CONFLICT".equals(e.getErrorType())));
        }

        @Test
        @DisplayName("不同日的两门课 → 无冲突")
        void submitSchedule_differentDays_noConflict() {
            // Arrange — MWF vs TR
            Schedule schedule = createSchedule(1L, ScheduleState.DRAFT);
            CourseOffering o1 = createOffering(1L, "CS101");
            o1.setDays("MWF");
            o1.setTimes("09:00-10:00");
            CourseOffering o2 = createOffering(2L, "CS201");
            o2.setDays("TR");
            o2.setTimes("09:00-10:00");

            when(scheduleRepository.findByStudentIdAndSemester(STUDENT_ID, SEMESTER))
                    .thenReturn(Optional.of(schedule));
            when(itemRepository.findByScheduleId(1L)).thenReturn(List.of(
                    createScheduleItem(1L, 1L, ItemKind.PRIMARY, ItemStatus.SELECTED),
                    createScheduleItem(2L, 2L, ItemKind.PRIMARY, ItemStatus.SELECTED)));
            when(offeringRepository.findById(1L)).thenReturn(Optional.of(o1));
            when(offeringRepository.findById(2L)).thenReturn(Optional.of(o2));
            when(courseRepository.findById("CS101")).thenReturn(Optional.of(createCourse("CS101", "Intro CS")));
            when(courseRepository.findById("CS201")).thenReturn(Optional.of(createCourse("CS201", "Data Structures")));
            when(enrollmentRepository.countByOfferingIdAndStatus(anyLong(), any())).thenReturn(0L);

            // Act
            SubmitResult result = scheduleService.submitSchedule(STUDENT_ID, SEMESTER);

            // Assert — 两门都成功
            assertTrue(result.isSuccess());
            assertEquals(2, result.getResults().size());
            assertTrue(result.getErrors().isEmpty());
        }

        @Test
        @DisplayName("同日但不同时段的两门课 → 无冲突")
        void submitSchedule_sameDayDifferentTimes_noConflict() {
            // Arrange — 都在 MWF, 但一个 09:00-10:00, 一个 10:30-11:30
            Schedule schedule = createSchedule(1L, ScheduleState.DRAFT);
            CourseOffering o1 = createOffering(1L, "CS101");
            o1.setDays("MWF");
            o1.setTimes("09:00-10:00");
            CourseOffering o2 = createOffering(2L, "CS201");
            o2.setDays("MWF");
            o2.setTimes("10:30-11:30");

            when(scheduleRepository.findByStudentIdAndSemester(STUDENT_ID, SEMESTER))
                    .thenReturn(Optional.of(schedule));
            when(itemRepository.findByScheduleId(1L)).thenReturn(List.of(
                    createScheduleItem(1L, 1L, ItemKind.PRIMARY, ItemStatus.SELECTED),
                    createScheduleItem(2L, 2L, ItemKind.PRIMARY, ItemStatus.SELECTED)));
            when(offeringRepository.findById(1L)).thenReturn(Optional.of(o1));
            when(offeringRepository.findById(2L)).thenReturn(Optional.of(o2));
            when(courseRepository.findById("CS101")).thenReturn(Optional.of(createCourse("CS101", "Intro CS")));
            when(courseRepository.findById("CS201")).thenReturn(Optional.of(createCourse("CS201", "Data Structures")));
            when(enrollmentRepository.countByOfferingIdAndStatus(anyLong(), any())).thenReturn(0L);

            // Act
            SubmitResult result = scheduleService.submitSchedule(STUDENT_ID, SEMESTER);

            // Assert — 无时间冲突,两门都成功
            assertTrue(result.isSuccess());
            assertEquals(2, result.getResults().size());
        }

        @Test
        @DisplayName("MWF 和 MW 共有日期且时间重叠 → 时间冲突")
        void submitSchedule_partialDayOverlap_conflict() {
            // Arrange — MWF 与 MW 共有 M 和 W, 且 09:00-10:00 和 09:30-10:30 时间重叠
            Schedule schedule = createSchedule(1L, ScheduleState.DRAFT);
            CourseOffering o1 = createOffering(1L, "CS101");
            o1.setDays("MWF");
            o1.setTimes("09:00-10:00");
            CourseOffering o2 = createOffering(2L, "CS201");
            o2.setDays("MW");
            o2.setTimes("09:30-10:30");

            when(scheduleRepository.findByStudentIdAndSemester(STUDENT_ID, SEMESTER))
                    .thenReturn(Optional.of(schedule));
            when(itemRepository.findByScheduleId(1L)).thenReturn(List.of(
                    createScheduleItem(1L, 1L, ItemKind.PRIMARY, ItemStatus.SELECTED),
                    createScheduleItem(2L, 2L, ItemKind.PRIMARY, ItemStatus.SELECTED)));
            when(offeringRepository.findById(1L)).thenReturn(Optional.of(o1));
            when(offeringRepository.findById(2L)).thenReturn(Optional.of(o2));
            when(courseRepository.findById("CS101")).thenReturn(Optional.of(createCourse("CS101", "Intro CS")));
            when(courseRepository.findById("CS201")).thenReturn(Optional.of(createCourse("CS201", "Data Structures")));
            when(enrollmentRepository.countByOfferingIdAndStatus(anyLong(), any())).thenReturn(0L);

            // Act
            SubmitResult result = scheduleService.submitSchedule(STUDENT_ID, SEMESTER);

            // Assert — 冲突 (MWF 和 MW 共有 M/W, 且时间段有重叠)
            assertFalse(result.isSuccess());
            assertTrue(result.getErrors().stream()
                    .anyMatch(e -> "TIME_CONFLICT".equals(e.getErrorType())));
        }

        @Test
        @DisplayName("已 enrolled 课程与新选课程同日同时段 → TIME_CONFLICT")
        void submitSchedule_conflictWithEnrolled_returnsConflict() {
            // Arrange
            Schedule schedule = createSchedule(1L, ScheduleState.DRAFT);
            CourseOffering o1 = createOffering(1L, "CS101");
            o1.setDays("MWF");
            o1.setTimes("09:00-10:00");
            CourseOffering o2 = createOffering(2L, "CS201");
            o2.setDays("MWF");
            o2.setTimes("09:00-10:00");

            ScheduleItem item1 = createScheduleItem(1L, 1L, ItemKind.PRIMARY, ItemStatus.ENROLLED_IN);
            ScheduleItem item2 = createScheduleItem(2L, 2L, ItemKind.PRIMARY, ItemStatus.SELECTED);

            when(scheduleRepository.findByStudentIdAndSemester(STUDENT_ID, SEMESTER))
                    .thenReturn(Optional.of(schedule));
            when(itemRepository.findByScheduleId(1L)).thenReturn(List.of(item1, item2));
            when(offeringRepository.findById(1L)).thenReturn(Optional.of(o1));
            when(offeringRepository.findById(2L)).thenReturn(Optional.of(o2));
            when(courseRepository.findById("CS101")).thenReturn(Optional.of(createCourse("CS101", "Intro CS")));
            when(courseRepository.findById("CS201")).thenReturn(Optional.of(createCourse("CS201", "Data Structures")));
            when(enrollmentRepository.countByOfferingIdAndStatus(anyLong(), any())).thenReturn(0L);

            // Act
            SubmitResult result = scheduleService.submitSchedule(STUDENT_ID, SEMESTER);

            // Assert — item2 与已 enrolled 的 item1 时间冲突
            assertFalse(result.isSuccess());
            assertTrue(result.getErrors().stream()
                    .anyMatch(e -> "TIME_CONFLICT".equals(e.getErrorType())));
        }

        @Test
        @DisplayName("null days 或 times → 不视为冲突")
        void submitSchedule_nullDaysTimes_noConflict() {
            // Arrange — 一门课没有 days/times 信息
            Schedule schedule = createSchedule(1L, ScheduleState.DRAFT);
            CourseOffering o1 = createOffering(1L, "CS101");
            o1.setDays(null);
            o1.setTimes(null);
            CourseOffering o2 = createOffering(2L, "CS201");
            o2.setDays("MWF");
            o2.setTimes("09:00-10:00");

            when(scheduleRepository.findByStudentIdAndSemester(STUDENT_ID, SEMESTER))
                    .thenReturn(Optional.of(schedule));
            when(itemRepository.findByScheduleId(1L)).thenReturn(List.of(
                    createScheduleItem(1L, 1L, ItemKind.PRIMARY, ItemStatus.SELECTED),
                    createScheduleItem(2L, 2L, ItemKind.PRIMARY, ItemStatus.SELECTED)));
            when(offeringRepository.findById(1L)).thenReturn(Optional.of(o1));
            when(offeringRepository.findById(2L)).thenReturn(Optional.of(o2));
            when(courseRepository.findById("CS101")).thenReturn(Optional.of(createCourse("CS101", "Intro CS")));
            when(courseRepository.findById("CS201")).thenReturn(Optional.of(createCourse("CS201", "Data Structures")));
            when(enrollmentRepository.countByOfferingIdAndStatus(anyLong(), any())).thenReturn(0L);

            // Act
            SubmitResult result = scheduleService.submitSchedule(STUDENT_ID, SEMESTER);

            // Assert — null times 不算冲突,两门都成功
            assertTrue(result.isSuccess());
            assertEquals(2, result.getResults().size());
        }
    }

    // ──────────────────────────────────────────────
    // 3. CAPACITY CONTROL
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("3. 容量控制")
    class CapacityTests {

        @Test
        @DisplayName("课程已满 → CAPACITY_FULL")
        void submitSchedule_offeringFull_returnsCapacityError() {
            // Arrange
            Schedule schedule = createSchedule(1L, ScheduleState.DRAFT);
            CourseOffering offering = createOffering(1L, "CS101");
            offering.setMaxCapacity(10);

            when(scheduleRepository.findByStudentIdAndSemester(STUDENT_ID, SEMESTER))
                    .thenReturn(Optional.of(schedule));
            when(itemRepository.findByScheduleId(1L)).thenReturn(List.of(
                    createScheduleItem(1L, 1L, ItemKind.PRIMARY, ItemStatus.SELECTED)));
            when(offeringRepository.findById(1L)).thenReturn(Optional.of(offering));
            when(courseRepository.findById("CS101")).thenReturn(Optional.of(createCourse("CS101", "Intro CS")));
            when(enrollmentRepository.countByOfferingIdAndStatus(1L, EnrollmentStatus.ENROLLED))
                    .thenReturn(10L);

            // Act
            SubmitResult result = scheduleService.submitSchedule(STUDENT_ID, SEMESTER);

            // Assert
            assertFalse(result.isSuccess());
            assertTrue(result.getErrors().stream()
                    .anyMatch(e -> "CAPACITY_FULL".equals(e.getErrorType())));
        }

        @Test
        @DisplayName("课程有空位 → 成功注册")
        void submitSchedule_offeringHasCapacity_enrolls() {
            // Arrange
            Schedule schedule = createSchedule(1L, ScheduleState.DRAFT);
            CourseOffering offering = createOffering(1L, "CS101");
            offering.setMaxCapacity(10);

            when(scheduleRepository.findByStudentIdAndSemester(STUDENT_ID, SEMESTER))
                    .thenReturn(Optional.of(schedule));
            when(itemRepository.findByScheduleId(1L)).thenReturn(List.of(
                    createScheduleItem(1L, 1L, ItemKind.PRIMARY, ItemStatus.SELECTED)));
            when(offeringRepository.findById(1L)).thenReturn(Optional.of(offering));
            when(courseRepository.findById("CS101")).thenReturn(Optional.of(createCourse("CS101", "Intro CS")));
            when(enrollmentRepository.countByOfferingIdAndStatus(1L, EnrollmentStatus.ENROLLED))
                    .thenReturn(5L);

            // Act
            SubmitResult result = scheduleService.submitSchedule(STUDENT_ID, SEMESTER);

            // Assert
            assertTrue(result.isSuccess());
            assertEquals(1, result.getResults().size());
            verify(enrollmentRepository).save(any(Enrollment.class));
        }

        @Test
        @DisplayName("课程人数恰好达到上限 → CAPACITY_FULL")
        void submitSchedule_offeringExactlyAtCapacity_returnsError() {
            // Arrange — 当前 10 人 == offeringMaxStudents(10)
            Schedule schedule = createSchedule(1L, ScheduleState.DRAFT);
            CourseOffering offering = createOffering(1L, "CS101");
            offering.setMaxCapacity(10);

            when(scheduleRepository.findByStudentIdAndSemester(STUDENT_ID, SEMESTER))
                    .thenReturn(Optional.of(schedule));
            when(itemRepository.findByScheduleId(1L)).thenReturn(List.of(
                    createScheduleItem(1L, 1L, ItemKind.PRIMARY, ItemStatus.SELECTED)));
            when(offeringRepository.findById(1L)).thenReturn(Optional.of(offering));
            when(courseRepository.findById("CS101")).thenReturn(Optional.of(createCourse("CS101", "Intro CS")));
            when(enrollmentRepository.countByOfferingIdAndStatus(1L, EnrollmentStatus.ENROLLED))
                    .thenReturn(10L);

            // Act
            SubmitResult result = scheduleService.submitSchedule(STUDENT_ID, SEMESTER);

            // Assert — 10 >= 10 → 满员
            assertFalse(result.isSuccess());
            assertTrue(result.getErrors().stream()
                    .anyMatch(e -> "CAPACITY_FULL".equals(e.getErrorType())));
        }
    }

    // ──────────────────────────────────────────────
    // 4. SCHEDULE OPERATIONS
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("4. 课表操作")
    class ScheduleOperationTests {

        @Test
        @DisplayName("创建课表 → 新建 DRAFT 状态课表")
        void createSchedule_new_success() {
            // Arrange — 本学期无课表
            when(scheduleRepository.findByStudentIdAndSemester(STUDENT_ID, SEMESTER))
                    .thenReturn(Optional.empty());
            when(scheduleRepository.save(any(Schedule.class))).thenAnswer(inv -> {
                Schedule s = inv.getArgument(0);
                s.setId(1L);
                return s;
            });
            when(itemRepository.findByScheduleId(1L)).thenReturn(List.of());

            // Act
            ScheduleResponse response = scheduleService.createSchedule(STUDENT_ID, SEMESTER);

            // Assert
            assertNotNull(response);
            assertEquals(ScheduleState.DRAFT, response.getState());
            assertEquals(STUDENT_ID, response.getStudentId());
            assertEquals(SEMESTER, response.getSemester());
            verify(scheduleRepository).save(any(Schedule.class));
        }

        @Test
        @DisplayName("创建课表 — 已存在 → 409 冲突")
        void createSchedule_alreadyExists_throwsConflict() {
            // Arrange — 已有课表
            Schedule existing = createSchedule(1L, ScheduleState.DRAFT);
            when(scheduleRepository.findByStudentIdAndSemester(STUDENT_ID, SEMESTER))
                    .thenReturn(Optional.of(existing));

            // Act & Assert
            ApiException ex = assertThrows(ApiException.class,
                    () -> scheduleService.createSchedule(STUDENT_ID, SEMESTER));
            assertEquals(409, ex.getStatus());
        }

        @Test
        @DisplayName("更新课表 (selectOfferings) → 添加课程项")
        void selectOfferings_addsItems() {
            // Arrange
            Schedule schedule = createSchedule(1L, ScheduleState.DRAFT);
            CourseOffering offering = createOffering(1L, "CS101");

            when(scheduleRepository.findByStudentIdAndSemester(STUDENT_ID, SEMESTER))
                    .thenReturn(Optional.of(schedule));
            when(itemRepository.findByScheduleId(1L)).thenReturn(List.of());
            when(offeringRepository.findById(1L)).thenReturn(Optional.of(offering));
            when(itemRepository.save(any(ScheduleItem.class))).thenAnswer(inv -> inv.getArgument(0));

            SelectOfferingsRequest request = new SelectOfferingsRequest();
            SelectOfferingsRequest.OfferingSelection sel = new SelectOfferingsRequest.OfferingSelection();
            sel.setOfferingId(1L);
            sel.setKind(ItemKind.PRIMARY);
            request.setSelections(List.of(sel));

            // Act
            ScheduleResponse response = scheduleService.selectOfferings(STUDENT_ID, SEMESTER, request);

            // Assert
            assertNotNull(response);
            verify(itemRepository).save(any(ScheduleItem.class));
        }

        @Test
        @DisplayName("删除课表 → 移除所有项和 enrolled 记录")
        void deleteSchedule_withEnrolledItems_removesAll() {
            // Arrange
            Schedule schedule = createSchedule(1L, ScheduleState.DRAFT);
            ScheduleItem item = createScheduleItem(1L, 1L, ItemKind.PRIMARY, ItemStatus.ENROLLED_IN);

            when(scheduleRepository.findByStudentIdAndSemester(STUDENT_ID, SEMESTER))
                    .thenReturn(Optional.of(schedule));
            when(itemRepository.findByScheduleId(1L)).thenReturn(List.of(item));
            when(enrollmentRepository.findByOfferingIdAndStatus(1L, EnrollmentStatus.ENROLLED))
                    .thenReturn(List.of(createEnrollment(1L, 1L, STUDENT_ID)));

            // Act
            scheduleService.deleteSchedule(STUDENT_ID, SEMESTER);

            // Assert
            verify(enrollmentRepository).save(any(Enrollment.class));
            verify(itemRepository).deleteAll(anyList());
            verify(scheduleRepository).delete(schedule);
            verify(auditService).recordScheduleDelete(STUDENT_ID, 1L, SEMESTER);
        }

        @Test
        @DisplayName("删除课表 — CLOSED 状态 → 409")
        void deleteSchedule_closedState_throwsConflict() {
            // Arrange
            Schedule schedule = createSchedule(1L, ScheduleState.CLOSED);
            when(scheduleRepository.findByStudentIdAndSemester(STUDENT_ID, SEMESTER))
                    .thenReturn(Optional.of(schedule));

            // Act & Assert
            ApiException ex = assertThrows(ApiException.class,
                    () -> scheduleService.deleteSchedule(STUDENT_ID, SEMESTER));
            assertEquals(409, ex.getStatus());
        }

        @Test
        @DisplayName("保存课表 (不提交) → 返回当前快照")
        void saveSchedule_returnsCurrentSnapshot() {
            // Arrange
            Schedule schedule = createSchedule(1L, ScheduleState.DRAFT);
            when(scheduleRepository.findByStudentIdAndSemester(STUDENT_ID, SEMESTER))
                    .thenReturn(Optional.of(schedule));
            when(itemRepository.findByScheduleId(1L)).thenReturn(List.of());

            // Act
            ScheduleResponse response = scheduleService.saveSchedule(STUDENT_ID, SEMESTER);

            // Assert
            assertNotNull(response);
            assertEquals(ScheduleState.DRAFT, response.getState());
            // saveSchedule 不改变状态,不调用 scheduleRepository.save
            verify(scheduleRepository, never()).save(any());
        }

        @Test
        @DisplayName("提交课表 — 全部成功 → 状态变为 COMMITTED")
        void submitSchedule_allSuccess_commits() {
            // Arrange — 一门无先修课, 不同时间段, 有空位
            Schedule schedule = createSchedule(1L, ScheduleState.DRAFT);
            CourseOffering offering = createOffering(1L, "CS101");

            when(scheduleRepository.findByStudentIdAndSemester(STUDENT_ID, SEMESTER))
                    .thenReturn(Optional.of(schedule));
            when(itemRepository.findByScheduleId(1L)).thenReturn(List.of(
                    createScheduleItem(1L, 1L, ItemKind.PRIMARY, ItemStatus.SELECTED)));
            when(offeringRepository.findById(1L)).thenReturn(Optional.of(offering));
            when(courseRepository.findById("CS101")).thenReturn(Optional.of(createCourse("CS101", "Intro CS")));
            when(enrollmentRepository.countByOfferingIdAndStatus(1L, EnrollmentStatus.ENROLLED))
                    .thenReturn(3L);

            // Act
            SubmitResult result = scheduleService.submitSchedule(STUDENT_ID, SEMESTER);

            // Assert
            assertTrue(result.isSuccess());
            assertEquals(ScheduleState.COMMITTED, schedule.getState());
            verify(scheduleRepository).save(schedule);
            verify(auditService).recordScheduleSubmit(eq(STUDENT_ID), eq(1L), eq(SEMESTER),
                    eq(true), eq("全部成功"));
        }

        @Test
        @DisplayName("提交课表 — 部分失败 → 课表保持 DRAFT")
        void submitSchedule_partialFailure_staysDraft() {
            // Arrange — CS101 无先修课+不同时间段可成功, CS201 需要 CS101 但学生没通过
            Schedule schedule = createSchedule(1L, ScheduleState.DRAFT);
            CourseOffering o1 = createOffering(1L, "CS101");
            o1.setDays("MWF");
            o1.setTimes("09:00-10:00");
            CourseOffering o2 = createOffering(2L, "CS201");
            o2.setDays("TR");
            o2.setTimes("09:00-10:00");
            Course c2 = createCourse("CS201", "Data Structures", "CS101");

            when(scheduleRepository.findByStudentIdAndSemester(STUDENT_ID, SEMESTER))
                    .thenReturn(Optional.of(schedule));
            when(itemRepository.findByScheduleId(1L)).thenReturn(List.of(
                    createScheduleItem(1L, 1L, ItemKind.PRIMARY, ItemStatus.SELECTED),
                    createScheduleItem(2L, 2L, ItemKind.PRIMARY, ItemStatus.SELECTED)));
            when(offeringRepository.findById(1L)).thenReturn(Optional.of(o1));
            when(offeringRepository.findById(2L)).thenReturn(Optional.of(o2));
            when(courseRepository.findById("CS101")).thenReturn(Optional.of(createCourse("CS101", "Intro CS")));
            when(courseRepository.findById("CS201")).thenReturn(Optional.of(c2));
            when(enrollmentRepository.countByOfferingIdAndStatus(anyLong(), any())).thenReturn(0L);

            // 学生没有 CS101 的及格成绩, CS201 先修课不满足
            when(gradeRepository.findByStudentId(STUDENT_ID)).thenReturn(List.of());

            // Act
            SubmitResult result = scheduleService.submitSchedule(STUDENT_ID, SEMESTER);

            // Assert — CS101 成功, CS201 先修课失败, 课表保持 DRAFT
            assertFalse(result.isSuccess());
            assertEquals(ScheduleState.DRAFT, schedule.getState());
            assertEquals(1, result.getResults().size());
            assertEquals(1, result.getErrors().size());
            assertEquals("PREREQUISITE_NOT_MET", result.getErrors().get(0).getErrorType());
        }

        @Test
        @DisplayName("更新课表 — COMMITTED 状态 → 409 无法修改")
        void selectOfferings_committedState_throwsConflict() {
            // Arrange
            Schedule schedule = createSchedule(1L, ScheduleState.COMMITTED);
            when(scheduleRepository.findByStudentIdAndSemester(STUDENT_ID, SEMESTER))
                    .thenReturn(Optional.of(schedule));

            SelectOfferingsRequest request = new SelectOfferingsRequest();
            SelectOfferingsRequest.OfferingSelection sel = new SelectOfferingsRequest.OfferingSelection();
            sel.setOfferingId(1L);
            sel.setKind(ItemKind.PRIMARY);
            request.setSelections(List.of(sel));

            // Act & Assert
            ApiException ex = assertThrows(ApiException.class,
                    () -> scheduleService.selectOfferings(STUDENT_ID, SEMESTER, request));
            assertEquals(409, ex.getStatus());
        }
    }

    // ──────────────────────────────────────────────
    // 5. BUSINESS RULES
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("5. 业务规则")
    class BusinessRuleTests {

        @Test
        @DisplayName("主选超过 4 门 → 报错")
        void selectOfferings_exceedsMaxPrimary_throwsBadRequest() {
            // Arrange — 已有 4 门主选
            Schedule schedule = createSchedule(1L, ScheduleState.DRAFT);
            List<ScheduleItem> existingItems = new ArrayList<>();
            for (long i = 1; i <= 4; i++) {
                existingItems.add(createScheduleItem(i, i, ItemKind.PRIMARY, ItemStatus.SELECTED));
            }

            when(scheduleRepository.findByStudentIdAndSemester(STUDENT_ID, SEMESTER))
                    .thenReturn(Optional.of(schedule));
            when(itemRepository.findByScheduleId(1L)).thenReturn(existingItems);

            // 要再加 1 门主选
            CourseOffering offering = createOffering(10L, "CS301");
            when(offeringRepository.findById(10L)).thenReturn(Optional.of(offering));

            SelectOfferingsRequest request = new SelectOfferingsRequest();
            SelectOfferingsRequest.OfferingSelection sel = new SelectOfferingsRequest.OfferingSelection();
            sel.setOfferingId(10L);
            sel.setKind(ItemKind.PRIMARY);
            request.setSelections(List.of(sel));

            // Act & Assert
            ApiException ex = assertThrows(ApiException.class,
                    () -> scheduleService.selectOfferings(STUDENT_ID, SEMESTER, request));
            assertTrue(ex.getMessage().contains("主选课程不能超过"));
        }

        @Test
        @DisplayName("备选超过 2 门 → 报错")
        void selectOfferings_exceedsMaxAlternate_throwsBadRequest() {
            // Arrange — 已有 2 门备选
            Schedule schedule = createSchedule(1L, ScheduleState.DRAFT);
            List<ScheduleItem> existingItems = List.of(
                    createScheduleItem(1L, 1L, ItemKind.ALTERNATE, ItemStatus.SELECTED),
                    createScheduleItem(2L, 2L, ItemKind.ALTERNATE, ItemStatus.SELECTED));

            when(scheduleRepository.findByStudentIdAndSemester(STUDENT_ID, SEMESTER))
                    .thenReturn(Optional.of(schedule));
            when(itemRepository.findByScheduleId(1L)).thenReturn(existingItems);

            // 要再加 1 门备选
            CourseOffering offering = createOffering(10L, "CS301");
            when(offeringRepository.findById(10L)).thenReturn(Optional.of(offering));

            SelectOfferingsRequest request = new SelectOfferingsRequest();
            SelectOfferingsRequest.OfferingSelection sel = new SelectOfferingsRequest.OfferingSelection();
            sel.setOfferingId(10L);
            sel.setKind(ItemKind.ALTERNATE);
            request.setSelections(List.of(sel));

            // Act & Assert
            ApiException ex = assertThrows(ApiException.class,
                    () -> scheduleService.selectOfferings(STUDENT_ID, SEMESTER, request));
            assertTrue(ex.getMessage().contains("备选课程不能超过"));
        }

        @Test
        @DisplayName("提交空课表 (无主选) → 返回空错误")
        void submitSchedule_noPrimaryItems_returnsEmptyError() {
            // Arrange — 课表有 items 但全是 ALTERNATE
            Schedule schedule = createSchedule(1L, ScheduleState.DRAFT);
            when(scheduleRepository.findByStudentIdAndSemester(STUDENT_ID, SEMESTER))
                    .thenReturn(Optional.of(schedule));
            when(itemRepository.findByScheduleId(1L)).thenReturn(List.of(
                    createScheduleItem(1L, 1L, ItemKind.ALTERNATE, ItemStatus.SELECTED)));

            // Act
            SubmitResult result = scheduleService.submitSchedule(STUDENT_ID, SEMESTER);

            // Assert — primaryCount == 0 → 课表为空
            assertFalse(result.isSuccess());
            assertEquals("课表为空,请先选择课程", result.getMessage());
        }

        @Test
        @DisplayName("注册已关闭时创建课表 → 409")
        void createSchedule_registrationClosed_throwsConflict() {
            // Arrange — service uses assertScheduleCreationAllowed() which checks isOpen()
            when(registrationPhase.isOpen()).thenReturn(false);
            when(registrationPhase.isAddDrop()).thenReturn(false);

            // Act & Assert
            ApiException ex = assertThrows(ApiException.class,
                    () -> scheduleService.createSchedule(STUDENT_ID, SEMESTER));
            assertEquals(409, ex.getStatus());
            assertTrue(ex.getMessage().contains("注册已关闭"));
        }

        @Test
        @DisplayName("注册已关闭时提交课表 → 409")
        void submitSchedule_registrationClosed_throwsConflict() {
            // Arrange — service uses assertRegistrationActive() which checks isClosed()
            when(registrationPhase.isClosed()).thenReturn(true);

            // Act & Assert
            ApiException ex = assertThrows(ApiException.class,
                    () -> scheduleService.submitSchedule(STUDENT_ID, SEMESTER));
            assertEquals(409, ex.getStatus());
        }

        @Test
        @DisplayName("selectOfferings — 课程状态非 OPEN → 400")
        void selectOfferings_offeringNotOpen_throwsBadRequest() {
            // Arrange
            Schedule schedule = createSchedule(1L, ScheduleState.DRAFT);
            CourseOffering offering = createOffering(1L, "CS101");
            offering.setStatus(OfferingStatus.CANCELLED);

            when(scheduleRepository.findByStudentIdAndSemester(STUDENT_ID, SEMESTER))
                    .thenReturn(Optional.of(schedule));
            when(itemRepository.findByScheduleId(1L)).thenReturn(List.of());
            when(offeringRepository.findById(1L)).thenReturn(Optional.of(offering));

            SelectOfferingsRequest request = new SelectOfferingsRequest();
            SelectOfferingsRequest.OfferingSelection sel = new SelectOfferingsRequest.OfferingSelection();
            sel.setOfferingId(1L);
            sel.setKind(ItemKind.PRIMARY);
            request.setSelections(List.of(sel));

            // Act & Assert
            ApiException ex = assertThrows(ApiException.class,
                    () -> scheduleService.selectOfferings(STUDENT_ID, SEMESTER, request));
            assertEquals(400, ex.getStatus());
        }

        @Test
        @DisplayName("selectOfferings — 课程不存在 → 404")
        void selectOfferings_offeringNotFound_throwsNotFound() {
            // Arrange
            Schedule schedule = createSchedule(1L, ScheduleState.DRAFT);

            when(scheduleRepository.findByStudentIdAndSemester(STUDENT_ID, SEMESTER))
                    .thenReturn(Optional.of(schedule));
            when(itemRepository.findByScheduleId(1L)).thenReturn(List.of());
            when(offeringRepository.findById(999L)).thenReturn(Optional.empty());

            SelectOfferingsRequest request = new SelectOfferingsRequest();
            SelectOfferingsRequest.OfferingSelection sel = new SelectOfferingsRequest.OfferingSelection();
            sel.setOfferingId(999L);
            sel.setKind(ItemKind.PRIMARY);
            request.setSelections(List.of(sel));

            // Act & Assert
            ApiException ex = assertThrows(ApiException.class,
                    () -> scheduleService.selectOfferings(STUDENT_ID, SEMESTER, request));
            assertEquals(404, ex.getStatus());
        }
    }

    // ==================== Helper Methods ====================

    private Schedule createSchedule(Long id, ScheduleState state) {
        Schedule schedule = new Schedule();
        schedule.setId(id);
        schedule.setStudentId(STUDENT_ID);
        schedule.setSemester(SEMESTER);
        schedule.setState(state);
        return schedule;
    }

    private CourseOffering createOffering(Long id, String courseId) {
        CourseOffering offering = new CourseOffering();
        offering.setId(id);
        offering.setCourseId(courseId);
        offering.setSemester(SEMESTER);
        offering.setProfessorId(1L);
        offering.setStatus(OfferingStatus.OPEN);
        offering.setMaxCapacity(10);
        offering.setDays("MWF");
        offering.setTimes("09:00-10:00");
        offering.setRoom("Room 101");
        return offering;
    }

    private Course createCourse(String courseId, String title) {
        Course course = new Course();
        course.setCourseId(courseId);
        course.setTitle(title);
        course.setCredits(3);
        return course;
    }

    private Course createCourse(String courseId, String title, String prerequisiteId) {
        Course course = createCourse(courseId, title);
        course.setPrerequisiteId(prerequisiteId);
        return course;
    }

    private ScheduleItem createScheduleItem(Long id, Long offeringId, ItemKind kind,
                                            ItemStatus status) {
        ScheduleItem item = new ScheduleItem();
        item.setId(id);
        item.setScheduleId(1L);
        item.setOfferingId(offeringId);
        item.setKind(kind);
        item.setStatus(status);
        return item;
    }

    private Enrollment createEnrollment(Long id, Long offeringId, Long studentId) {
        Enrollment enrollment = new Enrollment();
        enrollment.setId(id);
        enrollment.setOfferingId(offeringId);
        enrollment.setStudentId(studentId);
        enrollment.setStatus(EnrollmentStatus.ENROLLED);
        return enrollment;
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
