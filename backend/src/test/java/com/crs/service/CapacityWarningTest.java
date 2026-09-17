package com.crs.service;

import com.crs.common.ApiException;
import com.crs.common.RegistrationPhase;
import com.crs.domain.Course;
import com.crs.domain.CourseOffering;
import com.crs.domain.EnrollmentStatus;
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
import com.crs.web.dto.CapacityWarning;
import com.crs.web.dto.ScheduleResponse;
import com.crs.web.dto.SelectOfferingsRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
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
 * 课程满员通知功能单元测试。
 * <p>
 * 测试 selectOfferings 返回容量警告列表。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CapacityWarningTest {

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

    private static final Long STUDENT_ID = 1L;
    private static final String SEMESTER = "2026-FALL";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduleService, "maxPrimary", 4);
        ReflectionTestUtils.setField(scheduleService, "maxAlternate", 2);
        ReflectionTestUtils.setField(scheduleService, "offeringMaxStudents", 10);
        ReflectionTestUtils.setField(scheduleService, "offeringMinStudents", 3);

        lenient().when(registrationPhase.isOpen()).thenReturn(true);
        lenient().when(registrationPhase.isAddDrop()).thenReturn(false);
        lenient().when(registrationPhase.isClosed()).thenReturn(false);
    }

    @Test
    @DisplayName("selectOfferings 课程已满 → 返回容量警告")
    void selectOfferings_offeringFull_returnsCapacityWarning() {
        // Arrange
        Schedule schedule = createSchedule(1L, ScheduleState.DRAFT);
        CourseOffering offering = createOffering(1L, "CS101");
        offering.setMaxCapacity(10);

        // 跟踪已保存的 item
        List<ScheduleItem> savedItems = new CopyOnWriteArrayList<>();

        when(scheduleRepository.findByStudentIdAndSemester(STUDENT_ID, SEMESTER))
                .thenReturn(Optional.of(schedule));

        // 模拟: 第一次调用返回空(初始查询), 后续调用返回已保存的 items
        lenient().doAnswer(inv -> {
            Long scheduleId = inv.getArgument(0);
            return savedItems.stream()
                    .filter(i -> i.getScheduleId().equals(scheduleId))
                    .toList();
        }).when(itemRepository).findByScheduleId(anyLong());

        when(offeringRepository.findById(1L)).thenReturn(Optional.of(offering));

        when(itemRepository.save(any(ScheduleItem.class))).thenAnswer(inv -> {
            ScheduleItem item = inv.getArgument(0);
            if (item.getId() == null) {
                item.setId(System.nanoTime());
            }
            savedItems.add(item);
            return item;
        });

        // 模拟已满员 (10/10)
        when(enrollmentRepository.countByOfferingIdAndStatus(1L, EnrollmentStatus.ENROLLED))
                .thenReturn(10L);

        SelectOfferingsRequest request = new SelectOfferingsRequest();
        SelectOfferingsRequest.OfferingSelection sel = new SelectOfferingsRequest.OfferingSelection();
        sel.setOfferingId(1L);
        sel.setKind(ItemKind.PRIMARY);
        request.setSelections(List.of(sel));

        // Act
        ScheduleResponse response = scheduleService.selectOfferings(STUDENT_ID, SEMESTER, request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getCapacityWarnings());
        assertEquals(1, response.getCapacityWarnings().size());

        CapacityWarning warning = response.getCapacityWarnings().get(0);
        assertEquals(1L, warning.getOfferingId());
        assertEquals("CS101", warning.getCourseId());
        assertEquals(10, warning.getCurrentEnrolled());
        assertEquals(10, warning.getMaxCapacity());
        assertTrue(warning.getMessage().contains("已满员"));
    }

    @Test
    @DisplayName("selectOfferings 课程接近满员 → 返回接近满员警告")
    void selectOfferings_offeringNearFull_returnsNearFullWarning() {
        // Arrange
        Schedule schedule = createSchedule(1L, ScheduleState.DRAFT);
        CourseOffering offering = createOffering(1L, "CS101");
        offering.setMaxCapacity(10);

        List<ScheduleItem> savedItems = new CopyOnWriteArrayList<>();

        when(scheduleRepository.findByStudentIdAndSemester(STUDENT_ID, SEMESTER))
                .thenReturn(Optional.of(schedule));
        lenient().doAnswer(inv -> {
            Long scheduleId = inv.getArgument(0);
            return savedItems.stream()
                    .filter(i -> i.getScheduleId().equals(scheduleId))
                    .toList();
        }).when(itemRepository).findByScheduleId(anyLong());

        when(offeringRepository.findById(1L)).thenReturn(Optional.of(offering));
        when(itemRepository.save(any(ScheduleItem.class))).thenAnswer(inv -> {
            ScheduleItem item = inv.getArgument(0);
            if (item.getId() == null) {
                item.setId(System.nanoTime());
            }
            savedItems.add(item);
            return item;
        });

        // 模拟接近满员 (9/10)
        when(enrollmentRepository.countByOfferingIdAndStatus(1L, EnrollmentStatus.ENROLLED))
                .thenReturn(9L);

        SelectOfferingsRequest request = new SelectOfferingsRequest();
        SelectOfferingsRequest.OfferingSelection sel = new SelectOfferingsRequest.OfferingSelection();
        sel.setOfferingId(1L);
        sel.setKind(ItemKind.PRIMARY);
        request.setSelections(List.of(sel));

        // Act
        ScheduleResponse response = scheduleService.selectOfferings(STUDENT_ID, SEMESTER, request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getCapacityWarnings());
        assertEquals(1, response.getCapacityWarnings().size());

        CapacityWarning warning = response.getCapacityWarnings().get(0);
        assertEquals(1L, warning.getOfferingId());
        assertEquals("CS101", warning.getCourseId());
        assertEquals(9, warning.getCurrentEnrolled());
        assertEquals(10, warning.getMaxCapacity());
        assertTrue(warning.getMessage().contains("已接近满员"));
    }

    @Test
    @DisplayName("selectOfferings 课程有空位 → 无容量警告")
    void selectOfferings_offeringHasCapacity_noWarnings() {
        // Arrange
        Schedule schedule = createSchedule(1L, ScheduleState.DRAFT);
        CourseOffering offering = createOffering(1L, "CS101");
        offering.setMaxCapacity(10);

        List<ScheduleItem> savedItems = new CopyOnWriteArrayList<>();

        when(scheduleRepository.findByStudentIdAndSemester(STUDENT_ID, SEMESTER))
                .thenReturn(Optional.of(schedule));
        lenient().doAnswer(inv -> {
            Long scheduleId = inv.getArgument(0);
            return savedItems.stream()
                    .filter(i -> i.getScheduleId().equals(scheduleId))
                    .toList();
        }).when(itemRepository).findByScheduleId(anyLong());

        when(offeringRepository.findById(1L)).thenReturn(Optional.of(offering));
        when(itemRepository.save(any(ScheduleItem.class))).thenAnswer(inv -> {
            ScheduleItem item = inv.getArgument(0);
            if (item.getId() == null) {
                item.setId(System.nanoTime());
            }
            savedItems.add(item);
            return item;
        });

        // 模拟有空位 (5/10)
        when(enrollmentRepository.countByOfferingIdAndStatus(1L, EnrollmentStatus.ENROLLED))
                .thenReturn(5L);

        SelectOfferingsRequest request = new SelectOfferingsRequest();
        SelectOfferingsRequest.OfferingSelection sel = new SelectOfferingsRequest.OfferingSelection();
        sel.setOfferingId(1L);
        sel.setKind(ItemKind.PRIMARY);
        request.setSelections(List.of(sel));

        // Act
        ScheduleResponse response = scheduleService.selectOfferings(STUDENT_ID, SEMESTER, request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getCapacityWarnings());
        assertTrue(response.getCapacityWarnings().isEmpty());
    }

    @Test
    @DisplayName("selectOfferings 多门课程混合状态 → 返回对应的容量警告")
    void selectOfferings_multipleOfferingsMixedStatus_returnsCorrespondingWarnings() {
        // Arrange
        Schedule schedule = createSchedule(1L, ScheduleState.DRAFT);
        CourseOffering o1 = createOffering(1L, "CS101");
        o1.setMaxCapacity(10);
        CourseOffering o2 = createOffering(2L, "CS201");
        o2.setMaxCapacity(8);

        List<ScheduleItem> savedItems = new CopyOnWriteArrayList<>();

        when(scheduleRepository.findByStudentIdAndSemester(STUDENT_ID, SEMESTER))
                .thenReturn(Optional.of(schedule));
        lenient().doAnswer(inv -> {
            Long scheduleId = inv.getArgument(0);
            return savedItems.stream()
                    .filter(i -> i.getScheduleId().equals(scheduleId))
                    .toList();
        }).when(itemRepository).findByScheduleId(anyLong());

        // 模拟: CS101 满员, CS201 有空位
        when(enrollmentRepository.countByOfferingIdAndStatus(1L, EnrollmentStatus.ENROLLED))
                .thenReturn(10L);
        when(enrollmentRepository.countByOfferingIdAndStatus(2L, EnrollmentStatus.ENROLLED))
                .thenReturn(3L);

        when(offeringRepository.findById(1L)).thenReturn(Optional.of(o1));
        when(offeringRepository.findById(2L)).thenReturn(Optional.of(o2));

        when(itemRepository.save(any(ScheduleItem.class))).thenAnswer(inv -> {
            ScheduleItem item = inv.getArgument(0);
            if (item.getId() == null) {
                item.setId(System.nanoTime());
            }
            savedItems.add(item);
            return item;
        });

        // Act: 选择两门课
        SelectOfferingsRequest request = new SelectOfferingsRequest();
        SelectOfferingsRequest.OfferingSelection sel1 = new SelectOfferingsRequest.OfferingSelection();
        sel1.setOfferingId(1L);
        sel1.setKind(ItemKind.PRIMARY);
        SelectOfferingsRequest.OfferingSelection sel2 = new SelectOfferingsRequest.OfferingSelection();
        sel2.setOfferingId(2L);
        sel2.setKind(ItemKind.PRIMARY);
        request.setSelections(List.of(sel1, sel2));

        ScheduleResponse response = scheduleService.selectOfferings(STUDENT_ID, SEMESTER, request);

        // Assert: CS101 满员有警告, CS201 有空位无警告
        assertNotNull(response);
        assertEquals(1, response.getCapacityWarnings().size());
        assertEquals("CS101", response.getCapacityWarnings().get(0).getCourseId());
        assertTrue(response.getCapacityWarnings().get(0).getMessage().contains("已满员"));
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
}
