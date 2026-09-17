package com.crs.service;

import com.crs.common.ApiException;
import com.crs.common.RegistrationPhase;
import com.crs.common.TimeConflictUtil;
import com.crs.domain.Course;
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
import com.crs.repository.CourseRepository;
import com.crs.repository.EnrollmentRepository;
import com.crs.repository.GradeRepository;
import com.crs.repository.ScheduleItemRepository;
import com.crs.repository.ScheduleRepository;
import com.crs.web.dto.ScheduleResponse;
import com.crs.web.dto.SelectOfferingsRequest;
import com.crs.web.dto.SubmitResult;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 课表服务: 实现用例 7.2 Register for Courses。
 * <p>
 * 三种基本流:
 * - Create a Schedule: 从 Catalog 取可用开设列表 → Select Offerings → Submit Schedule
 * - Update a Schedule: 显示当前课表 → 显示可用开设 → 学生增删 → 更新课表 → Submit
 * - Delete a Schedule: 显示当前课表 → 学生确认删除 → 删除课表(含从 Roster 移除)
 * <p>
 * 备选流:
 * - Save a Schedule: 只保存不提交
 * - 校验失败: 先修课不满足/课程已满/时间冲突 → 报错,学生可另选或取消
 * - No Schedule Found: Update/Delete 时找不到课表 → 报错
 * - Course Catalog System Unavailable: 错误显示,用例终止
 * - Course Registration Closed: 注册已关闭 → 提示并终止
 * <p>
 * 参考 AI 生成,经人工核验与调整。核心校验顺序、并发控制方案由人工决定。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleItemRepository itemRepository;
    private final CourseOfferingRepository offeringRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CatalogService catalogService;
    private final GradeRepository gradeRepository;
    private final NotificationService notificationService;
    private final RegistrationPhase registrationPhase;
    private final AuditService auditService;

    @Value("${crs.rules.max-primary:4}")
    private int maxPrimary;

    @Value("${crs.rules.max-alternate:2}")
    private int maxAlternate;

    @Value("${crs.rules.offering-max-students:10}")
    private int offeringMaxStudents;

    @Value("${crs.rules.offering-min-students:3}")
    private int offeringMinStudents;

    // ==================== 查询 ====================

    /**
     * 查询学生当前学期课表(Update/Delete 流前置)。
     * 若不存在返回 empty(供控制器区分 No Schedule Found 备选流)。
     */
    @Transactional(readOnly = true)
    public Optional<ScheduleResponse> getMySchedule(Long studentId, String semester) {
        Optional<Schedule> schedule = scheduleRepository
                .findByStudentIdAndSemester(studentId, semester);
        return schedule.map(s -> toResponse(s, studentId));
    }

    // ==================== Create a Schedule ====================

    /**
     * 创建新课表(Create a Schedule 基本流)。
     * <p>
     * 若学生本学期已有课表,返回 409 冲突(用 Update 流程)。
     */
    @Transactional
    public ScheduleResponse createSchedule(Long studentId, String semester) {
        assertScheduleCreationAllowed();

        // 检查是否已存在
        if (scheduleRepository.findByStudentIdAndSemester(studentId, semester).isPresent()) {
            throw ApiException.conflict("本学期课表已存在,请使用更新接口");
        }

        Schedule schedule = new Schedule();
        schedule.setStudentId(studentId);
        schedule.setSemester(semester);
        schedule.setState(ScheduleState.DRAFT);
        scheduleRepository.save(schedule);
        log.info("学生 {} 创建了 {} 学期课表 (id={})", studentId, semester, schedule.getId());

        return toResponse(schedule, studentId);
    }

    // ==================== Select Offerings ====================

    /**
     * 选择课程开设(Select Offerings 子流)。
     * <p>
     * 业务规则: 4 主选 + 2 备选。
     * 学生从可用开设列表中选课后,系统据此创建/更新课表项(status=SELECTED)。
     */
    @Transactional
    public ScheduleResponse selectOfferings(Long studentId, String semester,
                                            SelectOfferingsRequest request) {
        assertRegistrationActive();

        Schedule schedule = scheduleRepository.findByStudentIdAndSemester(studentId, semester)
                .orElseThrow(() -> ApiException.notFound("课表不存在,请先创建课表"));

        if (schedule.getState() != ScheduleState.DRAFT) {
            throw ApiException.conflict("课表状态为 " + schedule.getState() + ",无法修改");
        }

        // 统计已存在的 PRIMARY/ALTERNATE 数量
        List<ScheduleItem> existing = itemRepository.findByScheduleId(schedule.getId());
        int primaryCount = (int) existing.stream()
                .filter(i -> i.getKind() == ItemKind.PRIMARY).count();
        int alternateCount = (int) existing.stream()
                .filter(i -> i.getKind() == ItemKind.ALTERNATE).count();

        // 校验数量限制
        Set<Long> existingOfferingIds = new HashSet<>();
        for (ScheduleItem i : existing) {
            existingOfferingIds.add(i.getOfferingId());
        }

        for (SelectOfferingsRequest.OfferingSelection sel : request.getSelections()) {
            // 重复选择跳过
            if (existingOfferingIds.contains(sel.getOfferingId())) {
                continue;
            }
            // 校验开设存在且开放
            CourseOffering offering = offeringRepository.findById(sel.getOfferingId())
                    .orElseThrow(() -> ApiException.notFound(
                            "课程开设不存在: " + sel.getOfferingId()));
            if (offering.getStatus() != OfferingStatus.OPEN) {
                throw ApiException.badRequest(
                        "课程 " + offering.getCourseId() + " 当前状态为 " + offering.getStatus()
                                + ",不可选");
            }
            // 校验学期匹配
            if (!offering.getSemester().equals(semester)) {
                throw ApiException.badRequest("课程开设学期不匹配");
            }

            // 数量限制
            if (sel.getKind() == ItemKind.PRIMARY) {
                primaryCount++;
                if (primaryCount > maxPrimary) {
                    throw ApiException.badRequest(
                            "主选课程不能超过 " + maxPrimary + " 门");
                }
            } else {
                alternateCount++;
                if (alternateCount > maxAlternate) {
                    throw ApiException.badRequest(
                            "备选课程不能超过 " + maxAlternate + " 门");
                }
            }

            ScheduleItem item = new ScheduleItem();
            item.setScheduleId(schedule.getId());
            item.setOfferingId(sel.getOfferingId());
            item.setKind(sel.getKind());
            item.setStatus(ItemStatus.SELECTED);
            itemRepository.save(item);
        }

        log.info("学生 {} 选择 {} 个开设写入课表 {}", studentId,
                request.getSelections().size(), schedule.getId());
        return toResponse(schedule, studentId);
    }

    /**
     * 从课表移除某开设(Update a Schedule 中的"删"动作)。
     */
    @Transactional
    public ScheduleResponse removeOffering(Long studentId, String semester, Long offeringId) {
        assertRegistrationActive();

        Schedule schedule = scheduleRepository.findByStudentIdAndSemester(studentId, semester)
                .orElseThrow(() -> ApiException.notFound("课表不存在"));

        // 水平越权防护: 验证课表属于当前学生
        if (!schedule.getStudentId().equals(studentId)) {
            throw ApiException.forbidden("无权操作此课表");
        }

        if (schedule.getState() != ScheduleState.DRAFT) {
            throw ApiException.conflict("课表状态不允许修改");
        }

        List<ScheduleItem> items = itemRepository.findByScheduleId(schedule.getId());
        for (ScheduleItem item : items) {
            if (item.getOfferingId().equals(offeringId)) {
                // 已 enrolled 的需同时从 Roster 移除
                if (item.getStatus() == ItemStatus.ENROLLED_IN) {
                    removeEnrollment(offeringId, studentId);
                }
                itemRepository.delete(item);
                log.info("学生 {} 从课表移除开设 {}", studentId, offeringId);
                break;
            }
        }
        return toResponse(schedule, studentId);
    }

    // ==================== Save a Schedule (备选流) ====================

    /**
     * 仅保存课表不提交(备选流 Save a Schedule)。
     * 未 enrolled 的课程保持 SELECTED 状态。
     */
    @Transactional
    public ScheduleResponse saveSchedule(Long studentId, String semester) {
        Schedule schedule = scheduleRepository.findByStudentIdAndSemester(studentId, semester)
                .orElseThrow(() -> ApiException.notFound("课表不存在"));
        // DRAFT 状态本身就是已保存,这里仅返回当前快照
        log.info("学生 {} 保存课表 (id={})", studentId, schedule.getId());
        return toResponse(schedule, studentId);
    }

    // ==================== Submit Schedule ====================

    /**
     * 提交课表(Submit Schedule 子流)。
     * <p>
     * 对课表中每门未标记 "enrolled in" 的开设,依次校验:
     * 1. 先修课满足
     * 2. 课程开放
     * 3. 容量(≤10 人上限)
     * 4. 时间冲突(同一学生课表内日期时间重叠检测)
     * <p>
     * 校验通过则加入开设,标记 enrolled_in,保存课表。
     * 失败则收集所有错误返回,学生可调整后重试。
     * <p>
     * 并发控制: 容量校验与写入在同一事务内完成(行锁/乐观锁)。
     */
    @Transactional
    public SubmitResult submitSchedule(Long studentId, String semester) {
        assertRegistrationActive();

        Schedule schedule = scheduleRepository.findByStudentIdAndSemester(studentId, semester)
                .orElseThrow(() -> ApiException.notFound("课表不存在"));

        if (schedule.getState() != ScheduleState.DRAFT) {
            throw ApiException.conflict("课表状态为 " + schedule.getState() + ",无法提交");
        }

        List<ScheduleItem> items = itemRepository.findByScheduleId(schedule.getId());
        List<SubmitResult.ValidationError> errors = new ArrayList<>();
        List<SubmitResult.EnrollmentResult> results = new ArrayList<>();

        // 校验主选数量(必须为 4,可少于但不强制——按需求是上限)
        long primaryCount = items.stream()
                .filter(i -> i.getKind() == ItemKind.PRIMARY).count();
        if (primaryCount == 0) {
            return new SubmitResult(false, "课表为空,请先选择课程",
                    results, List.of());
        }

        // 收集已 enrolled 的开设 ID(用于跳过)
        Set<Long> alreadyEnrolled = new HashSet<>();
        for (ScheduleItem item : items) {
            if (item.getStatus() == ItemStatus.ENROLLED_IN) {
                alreadyEnrolled.add(item.getOfferingId());
            }
        }

        // 收集该学生本学期所有 enrolled 的开设时间(用于时间冲突检测)
        Map<Long, CourseOffering> offeringMap = new HashMap<>();
        Set<String> enrolledTimeSlots = new HashSet<>();
        for (ScheduleItem item : items) {
            CourseOffering o = offeringRepository.findById(item.getOfferingId()).orElse(null);
            if (o == null) continue;
            offeringMap.put(item.getOfferingId(), o);
            if (item.getStatus() == ItemStatus.ENROLLED_IN) {
                enrolledTimeSlots.add(timeSlotKey(o.getDays(), o.getTimes()));
            }
        }

        // 按顺序校验每门未 enrolled 的开设
        for (ScheduleItem item : items) {
            if (alreadyEnrolled.contains(item.getOfferingId())) {
                continue;
            }

            CourseOffering offering = offeringMap.get(item.getOfferingId());
            if (offering == null) {
                offering = offeringRepository.findById(item.getOfferingId()).orElse(null);
                if (offering == null) {
                    errors.add(new SubmitResult.ValidationError(
                            item.getOfferingId(), null, "NOT_FOUND",
                            "课程开设不存在"));
                    continue;
                }
            }

            String courseId = offering.getCourseId();

            // 1. 课程开放状态校验
            if (offering.getStatus() != OfferingStatus.OPEN
                    && offering.getStatus() != OfferingStatus.COMMITTED) {
                errors.add(new SubmitResult.ValidationError(
                        item.getOfferingId(), courseId, "OFFERING_NOT_OPEN",
                        "课程 " + courseId + " 状态为 " + offering.getStatus()
                                + ",不可选"));
                continue;
            }

            // 2. 先修课校验
            Course course = courseRepository.findById(courseId).orElse(null);
            if (course != null && course.getPrerequisiteId() != null) {
                if (!hasPassedPrerequisite(studentId, course.getPrerequisiteId())) {
                    errors.add(new SubmitResult.ValidationError(
                            item.getOfferingId(), courseId, "PREREQUISITE_NOT_MET",
                            "未满足先修课要求: " + course.getPrerequisiteId()));
                    continue;
                }
            }

            // 3. 容量校验(关键: 使用悲观锁锁定开设行,防止并发选课超卖)
            CourseOffering lockedOffering = offeringRepository
                    .findByIdWithPessimisticLock(offering.getId())
                    .orElse(offering);
            long currentEnrolled = enrollmentRepository.countByOfferingIdAndStatus(
                    lockedOffering.getId(), EnrollmentStatus.ENROLLED);
            if (currentEnrolled >= offeringMaxStudents
                    || currentEnrolled >= lockedOffering.getMaxCapacity()) {
                errors.add(new SubmitResult.ValidationError(
                        item.getOfferingId(), courseId, "CAPACITY_FULL",
                        "课程 " + courseId + " 已满员("
                                + currentEnrolled + "/" + lockedOffering.getMaxCapacity() + ")"));
                continue;
            }

            // 4. 时间冲突校验(检测实际时间重叠)
            if (hasTimeConflict(offering, enrolledTimeSlots, items, offeringMap)) {
                errors.add(new SubmitResult.ValidationError(
                        item.getOfferingId(), courseId, "TIME_CONFLICT",
                        "课程 " + courseId + " 与已选课程时间冲突 ("
                                + offering.getDays() + " " + offering.getTimes() + ")"));
                // 通知学生选课失败
                notificationService.notifyEnrollmentFailed(studentId, offering.getId(),
                        courseId, "时间冲突");
                continue;
            }

            // 校验通过: 加入开设(Roster)
            Enrollment enrollment = new Enrollment();
            enrollment.setOfferingId(offering.getId());
            enrollment.setStudentId(studentId);
            enrollment.setStatus(EnrollmentStatus.ENROLLED);
            enrollmentRepository.save(enrollment);

            // 标记 enrolled_in
            item.setStatus(ItemStatus.ENROLLED_IN);
            itemRepository.save(item);

            enrolledTimeSlots.add(timeSlotKey(offering.getDays(), offering.getTimes()));
            results.add(new SubmitResult.EnrollmentResult(
                    offering.getId(), courseId, true, "已成功注册"));

            // 通知学生选课成功
            notificationService.notifyEnrollmentSuccess(studentId, offering.getId(), courseId);
        }

        if (errors.isEmpty()) {
            // 全部成功: 课表状态变为 COMMITTED
            schedule.setState(ScheduleState.COMMITTED);
            scheduleRepository.save(schedule);
            log.info("学生 {} 提交课表成功 (id={})", studentId, schedule.getId());
            auditService.recordScheduleSubmit(studentId, schedule.getId(), semester,
                    errors.isEmpty(), errors.isEmpty() ? "全部成功" : errors.size() + "项失败");
            return new SubmitResult(true, "课表提交成功", results, errors);
        } else {
            // 部分失败: 课表保持 DRAFT,学生可调整后重试
            log.info("学生 {} 提交课表部分失败,错误 {} 项", studentId, errors.size());
            auditService.recordScheduleSubmit(studentId, schedule.getId(), semester,
                    errors.isEmpty(), errors.isEmpty() ? "全部成功" : errors.size() + "项失败");
            return new SubmitResult(false,
                    "有 " + errors.size() + " 门课程校验失败,请调整后重试",
                    results, errors);
        }
    }

    // ==================== Delete a Schedule ====================

    /**
     * 删除课表(Delete a Schedule 基本流)。
     * <p>
     * 若含 enrolled_in 课程,须将学生从对应 Roster 移除。
     */
    @Transactional
    public void deleteSchedule(Long studentId, String semester) {
        assertRegistrationActive();

        Schedule schedule = scheduleRepository.findByStudentIdAndSemester(studentId, semester)
                .orElseThrow(() -> ApiException.notFound("课表不存在"));

        // 水平越权防护: 验证课表属于当前学生
        if (!schedule.getStudentId().equals(studentId)) {
            throw ApiException.forbidden("无权操作此课表");
        }

        if (schedule.getState() == ScheduleState.CLOSED) {
            throw ApiException.conflict("注册已关闭,无法删除课表");
        }

        // 移除所有 enrolled 项的 Roster 记录
        List<ScheduleItem> items = itemRepository.findByScheduleId(schedule.getId());
        for (ScheduleItem item : items) {
            if (item.getStatus() == ItemStatus.ENROLLED_IN) {
                removeEnrollment(item.getOfferingId(), studentId);
            }
        }
        itemRepository.deleteAll(items);
        scheduleRepository.delete(schedule);
        auditService.recordScheduleDelete(studentId, schedule.getId(), semester);
        log.info("学生 {} 删除了 {} 学期课表 (id={})", studentId, semester, schedule.getId());
    }

    // ==================== 内部方法 ====================

    /**
     * 注册是否处于活跃阶段(open 或 add-drop)。
     * 用于选课/退课/提交/删除操作: 除 closed 外均允许。
     */
    private void assertRegistrationActive() {
        if (registrationPhase.isClosed()) {
            throw ApiException.conflict("注册已关闭,无法选课");
        }
    }

    /**
     * 是否允许新建课表。
     * 仅 open 阶段允许; add-drop 阶段只能修改已有课表。
     */
    private void assertScheduleCreationAllowed() {
        if (!registrationPhase.isOpen()) {
            String msg = registrationPhase.isAddDrop()
                    ? "当前为增减课阶段,不允许新建课表"
                    : "注册已关闭,无法创建课表";
            throw ApiException.conflict(msg);
        }
    }

    /**
     * 学生是否通过先修课。
     * 查询 Grade 表中是否有该课程及格成绩 (A/B/C/D,排除 F/I/NULL)。
     * 当前学期正在选修的课程不算先修课已通过。
     */
    private boolean hasPassedPrerequisite(Long studentId, String prerequisiteCourseId) {
        List<com.crs.domain.Grade> grades = gradeRepository.findByStudentId(studentId);
        Set<String> passed = new HashSet<>();
        for (com.crs.domain.Grade g : grades) {
            String grade = g.getGrade();
            if (grade != null && !grade.isEmpty()
                    && !grade.equals("F") && !grade.equals("I")) {
                // 需查询该成绩对应开设的 courseId
                CourseOffering o = offeringRepository.findById(g.getOfferingId()).orElse(null);
                if (o != null) {
                    passed.add(o.getCourseId());
                }
            }
        }
        return passed.contains(prerequisiteCourseId);
    }

    /** 从开设 Roster 移除学生 */
    private void removeEnrollment(Long offeringId, Long studentId) {
        List<Enrollment> enrollments = enrollmentRepository
                .findByOfferingIdAndStatus(offeringId, EnrollmentStatus.ENROLLED);
        for (Enrollment e : enrollments) {
            if (e.getStudentId().equals(studentId)) {
                e.setStatus(EnrollmentStatus.CANCELLED);
                enrollmentRepository.save(e);
            }
        }
    }

    /** 时间槽 key(简化: days + times 拼接)。实际可更精细解析时间段。 */
    private String timeSlotKey(String days, String times) {
        return days + "|" + times;
    }

    /**
     * 检测时间冲突。
     * <p>
     * 冲突条件:
     * 1. 同一天有共同日期(如 MWF 和 MWF,或 MWF 和 TR)
     * 2. 时间段有重叠(简化: 相同时间段视为冲突)
     * <p>
     * 实际实现应解析时间段(如 "09:00-10:00")进行更精确的重叠检测。
     */
    private boolean hasTimeConflict(CourseOffering offering, Set<String> enrolledTimeSlots,
                                    List<ScheduleItem> items,
                                    Map<Long, CourseOffering> offeringMap) {
        String offeringKey = timeSlotKey(offering.getDays(), offering.getTimes());

        // 检查是否与已 enrolled 的课程冲突
        for (String enrolledKey : enrolledTimeSlots) {
            if (hasTimeOverlap(offering.getDays(), offering.getTimes(),
                    enrolledKey.split("\\|")[0], enrolledKey.split("\\|")[1])) {
                return true;
            }
        }

        // 检查是否与课表中其他待提交的课程冲突
        for (ScheduleItem item : items) {
            CourseOffering other = offeringMap.get(item.getOfferingId());
            if (other == null || other.getId().equals(offering.getId())) continue;
            if (item.getStatus() == ItemStatus.ENROLLED_IN) continue; // 已在 enrolledTimeSlots 中检查过

            if (hasTimeOverlap(offering.getDays(), offering.getTimes(),
                    other.getDays(), other.getTimes())) {
                return true;
            }
        }

        return false;
    }

    /**
     * 判断两个时间段是否有重叠(委托给 TimeConflictUtil)。
     * <p>
     * 冲突条件: 日期有交集 AND 时间有重叠。
     */
    private boolean hasTimeOverlap(String days1, String times1, String days2, String times2) {
        return TimeConflictUtil.isConflict(days1, times1, days2, times2);
    }

    /** 转换为响应 DTO */
    private ScheduleResponse toResponse(Schedule schedule, Long studentId) {
        List<ScheduleItem> items = itemRepository.findByScheduleId(schedule.getId());
        List<ScheduleResponse.ScheduleItemDTO> itemDtos = new ArrayList<>();
        int primary = 0, alternate = 0;

        for (ScheduleItem item : items) {
            CourseOffering o = offeringRepository.findById(item.getOfferingId()).orElse(null);
            if (o == null) continue;
            Course c = courseRepository.findById(o.getCourseId()).orElse(null);
            long enrolled = enrollmentRepository.countByOfferingIdAndStatus(
                    o.getId(), EnrollmentStatus.ENROLLED);
            String profName = null;
            if (o.getProfessorId() != null) {
                profName = offeringRepository.findById(item.getOfferingId())
                        .flatMap(off -> catalogService.getOffering(off.getId()))
                        .map(dto -> dto.getProfessorName()).orElse(null);
            }
            itemDtos.add(new ScheduleResponse.ScheduleItemDTO(
                    item.getId(),
                    item.getOfferingId(),
                    o.getCourseId(),
                    c != null ? c.getTitle() : null,
                    o.getDays(),
                    o.getTimes(),
                    o.getRoom(),
                    item.getKind(),
                    item.getStatus(),
                    o.getProfessorId(),
                    profName,
                    (int) enrolled,
                    o.getMaxCapacity()
            ));
            if (item.getKind() == ItemKind.PRIMARY) primary++;
            else alternate++;
        }

        return new ScheduleResponse(
                schedule.getId(),
                studentId,
                schedule.getSemester(),
                schedule.getState(),
                itemDtos,
                primary,
                alternate
        );
    }
}
