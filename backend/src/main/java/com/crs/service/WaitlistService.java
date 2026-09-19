package com.crs.service;

import com.crs.common.ApiException;
import com.crs.domain.Course;
import com.crs.domain.CourseOffering;
import com.crs.domain.Enrollment;
import com.crs.domain.EnrollmentStatus;
import com.crs.domain.OfferingStatus;
import com.crs.domain.User;
import com.crs.domain.Waitlist;
import com.crs.domain.WaitlistStatus;
import com.crs.repository.CourseOfferingRepository;
import com.crs.repository.CourseRepository;
import com.crs.repository.EnrollmentRepository;
import com.crs.repository.UserRepository;
import com.crs.repository.WaitlistRepository;
import com.crs.web.dto.CourseOfferingDTO;
import com.crs.web.dto.WaitlistDTO;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 候补名单服务: 处理学生在课程满员时排队等待递补。
 * <p>
 * 核心逻辑:
 * - joinWaitlist: 加入候补(position 自动递增)
 * - cancelWaitlist: 取消候补
 * - getMyWaitlists: 查看个人候补列表
 * - promoteNextStudent: 递补下一位(有人退选时调用)
 * <p>
 * 递补时检查: 课程仍在 OPEN 状态, 仍有容量, 学生满足先修课要求。
 */
@Slf4j
@Service
public class WaitlistService {

    private final WaitlistRepository waitlistRepository;
    private final CourseOfferingRepository offeringRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final CatalogService catalogService;
    private final NotificationService notificationService;
    private final com.crs.common.PrerequisiteChecker prerequisiteChecker;

    @Value("${crs.rules.offering-max-students:10}")
    private int offeringMaxStudents;

    public WaitlistService(
            WaitlistRepository waitlistRepository,
            CourseOfferingRepository offeringRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository,
            CatalogService catalogService,
            NotificationService notificationService,
            com.crs.common.PrerequisiteChecker prerequisiteChecker) {
        this.waitlistRepository = waitlistRepository;
        this.offeringRepository = offeringRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.catalogService = catalogService;
        this.notificationService = notificationService;
        this.prerequisiteChecker = prerequisiteChecker;
    }

    /**
     * 加入候补名单。
     * <p>
     * 校验:
     * 1. 课程开设存在且为 OPEN 状态
     * 2. 课程已满(不能直接注册)
     * 3. 学生未重复加入(WAITING 状态)
     * 4. 学生未已注册该课程
     * <p>
     * position 自动递增: 当前最大 position + 1。
     */
    @Transactional
    public WaitlistDTO joinWaitlist(Long studentId, Long offeringId) {
        // 1. 校验课程开设
        CourseOffering offering = offeringRepository.findById(offeringId)
                .orElseThrow(() -> ApiException.notFound("课程开设不存在: " + offeringId));

        if (offering.getStatus() != OfferingStatus.OPEN
                && offering.getStatus() != OfferingStatus.COMMITTED) {
            throw ApiException.badRequest("课程 " + offering.getCourseId()
                    + " 当前状态为 " + offering.getStatus() + ",不可加入候补");
        }

        // 2. 校验课程已满(否则应直接选课)
        long enrolled = enrollmentRepository.countByOfferingIdAndStatus(
                offeringId, EnrollmentStatus.ENROLLED);
        int maxCap = offering.getMaxCapacity() != null
                ? offering.getMaxCapacity() : offeringMaxStudents;
        if (enrolled < maxCap) {
            throw ApiException.badRequest("课程 " + offering.getCourseId()
                    + " 仍有空位(" + enrolled + "/" + maxCap
                    + "),请直接选课而非加入候补");
        }

        // 3. 校验学生未已注册该课程
        List<Enrollment> existingEnrollments = enrollmentRepository
                .findByOfferingIdAndStatus(offeringId, EnrollmentStatus.ENROLLED);
        for (Enrollment e : existingEnrollments) {
            if (e.getStudentId().equals(studentId)) {
                throw ApiException.conflict("您已注册该课程,无需加入候补");
            }
        }

        // 4. 校验学生未重复加入
        waitlistRepository.findByOfferingIdAndStudentIdAndStatus(
                offeringId, studentId, WaitlistStatus.WAITING)
                .ifPresent(w -> {
                    throw ApiException.conflict("您已在该课程候补名单中(位置: " + w.getPosition() + ")");
                });

        // 5. 创建候补记录
        Waitlist waitlist = new Waitlist();
        waitlist.setOfferingId(offeringId);
        waitlist.setStudentId(studentId);
        waitlist.setPosition(waitlistRepository.findMaxPositionByOfferingId(offeringId) + 1);
        waitlist.setStatus(WaitlistStatus.WAITING);
        waitlistRepository.save(waitlist);

        log.info("学生 {} 加入课程 {} 候补名单, 位置: {}", studentId, offering.getCourseId(),
                waitlist.getPosition());

        return toDTO(waitlist);
    }

    /**
     * 取消候补名单。
     */
    @Transactional
    public void cancelWaitlist(Long studentId, Long offeringId) {
        Waitlist waitlist = waitlistRepository
                .findByOfferingIdAndStudentIdAndStatus(offeringId, studentId, WaitlistStatus.WAITING)
                .orElseThrow(() -> ApiException.notFound("候补记录不存在"));

        waitlist.setStatus(WaitlistStatus.CANCELLED);
        waitlistRepository.save(waitlist);
        log.info("学生 {} 取消课程 {} 候补(位置: {})",
                studentId, waitlist.getOfferingId(), waitlist.getPosition());
    }

    /**
     * 查看学生的候补列表。
     */
    @Transactional(readOnly = true)
    public List<WaitlistDTO> getMyWaitlists(Long studentId) {
        List<Waitlist> waitlists = waitlistRepository
                .findByStudentIdAndStatusOrderByCreatedAtAsc(studentId, WaitlistStatus.WAITING);
        return waitlists.stream().map(this::toDTO).toList();
    }

    /**
     * 查看某课程的候补队列(教授/Registrar 用)。
     */
    @Transactional(readOnly = true)
    public List<WaitlistDTO> getOfferingWaitlist(Long offeringId) {
        List<Waitlist> waitlists = waitlistRepository
                .findByOfferingIdAndStatusOrderByPositionAsc(offeringId, WaitlistStatus.WAITING);
        return waitlists.stream().map(this::toDTO).toList();
    }

    /**
     * 判断指定用户是否为该课程开设的授课教授。用于候补队列的越权校验。
     */
    @Transactional(readOnly = true)
    public boolean isInstructorOfOffering(Long professorId, Long offeringId) {
        return offeringRepository.findById(offeringId)
                .map(o -> o.getProfessorId() != null && o.getProfessorId().equals(professorId))
                .orElse(false);
    }

    /**
     * 递补下一位学生(当有人退选时调用)。
     * <p>
     * 逻辑:
     * 1. 获取该开设候补中 WAITING 状态、position 最小的记录
     * 2. 检查课程仍在 OPEN 状态, 仍有容量
     * 3. 检查学生满足先修课要求
     * 4. 自动注册(创建 Enrollment) + 更新 Waitlist 状态为 ENROLLED
     * 5. 通知学生递补成功
     * <p>
     * 注意: 此方法由外部调用(如 ScheduleService 退选时), 不直接在 ScheduleService 中修改。
     * 返回 true 表示递补成功, false 表示无候补或校验失败。
     */
    @Transactional
    public boolean promoteNextStudent(Long offeringId) {
        List<Waitlist> waitingList = waitlistRepository
                .findByOfferingIdAndStatusOrderByPositionAsc(offeringId, WaitlistStatus.WAITING);

        if (waitingList.isEmpty()) {
            log.info("课程开设 {} 无候补学生,跳过递补", offeringId);
            return false;
        }

        CourseOffering offering = offeringRepository.findById(offeringId)
                .orElseThrow(() -> ApiException.notFound("课程开设不存在: " + offeringId));

        // 逐个检查候补学生,直到找到满足条件的
        for (Waitlist waitlist : waitingList) {
            // 检查课程状态
            if (offering.getStatus() != OfferingStatus.OPEN
                    && offering.getStatus() != OfferingStatus.COMMITTED) {
                log.info("课程 {} 状态为 {},停止递补", offering.getCourseId(), offering.getStatus());
                return false;
            }

            // 检查容量
            long enrolled = enrollmentRepository.countByOfferingIdAndStatus(
                    offeringId, EnrollmentStatus.ENROLLED);
            int maxCap = offering.getMaxCapacity() != null
                    ? offering.getMaxCapacity() : offeringMaxStudents;
            if (enrolled >= maxCap) {
                log.info("课程 {} 已满员({}/{}),停止递补",
                        offering.getCourseId(), enrolled, maxCap);
                return false;
            }

            // 检查先修课
            if (!hasPassedPrerequisite(waitlist.getStudentId(), offering.getCourseId())) {
                log.info("学生 {} 未满足先修课要求,跳过递补(课程 {})",
                        waitlist.getStudentId(), offering.getCourseId());
                continue;
            }

            // 执行递补
            Enrollment enrollment = new Enrollment();
            enrollment.setOfferingId(offeringId);
            enrollment.setStudentId(waitlist.getStudentId());
            enrollment.setStatus(EnrollmentStatus.ENROLLED);
            enrollmentRepository.save(enrollment);

            waitlist.setStatus(WaitlistStatus.ENROLLED);
            waitlistRepository.save(waitlist);

            // 通知学生
            notificationService.notifyWaitlistPromoted(
                    waitlist.getStudentId(), offeringId, offering.getCourseId());

            log.info("递补成功: 学生 {} 被注册到课程 {} (原候补位置: {})",
                    waitlist.getStudentId(), offering.getCourseId(), waitlist.getPosition());
            return true;
        }

        log.info("课程 {} 所有候补学生均不满足条件,停止递补", offering.getCourseId());
        return false;
    }

    // ==================== 内部方法 ====================

    /**
     * 检查学生是否通过先修课。委托给 PrerequisiteChecker。
     */
    private boolean hasPassedPrerequisite(Long studentId, String courseId) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null || course.getPrerequisiteId() == null) {
            return true; // 无先修课要求
        }
        return prerequisiteChecker.hasPassedPrerequisite(studentId, course.getPrerequisiteId());
    }

    private WaitlistDTO toDTO(Waitlist waitlist) {
        CourseOffering offering = offeringRepository.findById(waitlist.getOfferingId()).orElse(null);
        User student = userRepository.findById(waitlist.getStudentId()).orElse(null);

        String courseId = offering != null ? offering.getCourseId() : null;
        String courseTitle = null;
        if (offering != null) {
            try {
                courseTitle = catalogService.getOffering(offering.getId())
                        .map(CourseOfferingDTO::getCourseTitle).orElse(null);
            } catch (Exception e) {
                log.warn("查询课程信息时目录不可用: offeringId={}", waitlist.getOfferingId());
            }
        }

        return new WaitlistDTO(
                waitlist.getId(),
                waitlist.getOfferingId(),
                courseId,
                courseTitle,
                waitlist.getStudentId(),
                student != null ? student.getName() : null,
                waitlist.getPosition(),
                waitlist.getStatus(),
                waitlist.getCreatedAt()
        );
    }
}
