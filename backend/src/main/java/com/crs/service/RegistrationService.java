package com.crs.service;

import com.crs.common.ApiException;
import com.crs.common.RegistrationPhase;
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
import com.crs.repository.ScheduleItemRepository;
import com.crs.repository.ScheduleRepository;
import com.crs.repository.UserRepository;
import com.crs.web.dto.CloseRegistrationResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 注册周期管理服务: 实现用例 7.8 Close Registration。
 * <p>
 * 完整算法(对应需求第 7.8 节):
 * <ol>
 *   <li>检查注册是否仍在进行中: 是 → 提示 Registrar 并用例终止。</li>
 *   <li>对每个课程开设: 检查是否<b>已有教授登记授课</b>且<b>选课人数 ≥ 3</b>;
 *       满足则对每个包含它的课表<b>提交(commit)</b>该开设。</li>
 *   <li><b>课表均衡(Leveling)</b>: 对每份课表,若主选课数未满上限(4 门),
 *       按备选列表顺序尝试选入第一个可用备选开设;无可用备选则不替换。</li>
 *   <li><b>关闭与取消</b>: 系统关闭全部开设;此时仍不足 3 人的开设(部分可能因 leveling 刚被加入)
 *       <b>取消</b>,并在所有包含它的课表中移除。</li>
 *   <li><b>计费</b>: 计算每生本学期学费,向 Billing System 发送事务;
 *       Billing System 出账并附最终课表副本。</li>
 * </ol>
 * <p>
 * 备选流:
 * <ul>
 *   <li>No Professor for the Course Offering: 无教授 → 该开设直接取消,从相关课表移除。</li>
 *   <li>Billing System Unavailable: 按设定周期重发事务,直至 Billing System 恢复可用。</li>
 * </ul>
 * <p>
 * 参考 AI 生成,经人工核验与调整。Leveling 算法与二次取消逻辑由人工设计。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final CourseOfferingRepository offeringRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleItemRepository itemRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final RegistrationPhase registrationPhase;

    @Value("${crs.rules.offering-min-students:3}")
    private int minStudents;

    @Value("${crs.rules.offering-max-students:10}")
    private int maxStudents;

    @Value("${crs.rules.max-primary:4}")
    private int maxPrimary;

    /** 当前学期 */
    private static final String CURRENT_SEMESTER = "2026-FALL";

    /**
     * 获取当前注册阶段状态。
     */
    public String getCurrentPhase() {
        return registrationPhase.getPhase();
    }

    /**
     * 关闭注册(完整基本流)。
     * <p>
     * 步骤:
     * 1. 校验当前状态是否允许关闭
     * 2. 检查教授指派 + 最小人数,决定 commit / cancel
     * 3. Leveling: 为学生课表补充备选课程
     * 4. 二次检查: 取消仍不足最小人数的开设
     * 5. 通知 Billing 系统
     */
    @Transactional
    public CloseRegistrationResult closeRegistration(String semester) {
        // ========== 步骤 1: 校验注册状态 ==========
        if ("closed".equalsIgnoreCase(registrationPhase.getPhase())) {
            throw ApiException.conflict("注册已关闭,不能重复关闭");
        }

        log.info("===== 注册主任发起关闭注册: semester={} =====", semester);
        registrationPhase.setPhase("closed");

        // ========== 步骤 2: 检查教授指派 + 最小人数,决定 commit / cancel ==========
        List<CourseOffering> allOfferings = offeringRepository
                .findBySemesterAndStatus(semester, OfferingStatus.OPEN);

        List<CloseRegistrationResult.OfferingStatus> offeringStatuses = new ArrayList<>();
        int committedCount = 0;
        int cancelledForNoProfessor = 0;
        int cancelledForUnderMin = 0;
        List<Enrollment> billableEnrollments = new ArrayList<>();

        for (CourseOffering offering : allOfferings) {
            List<Enrollment> enrollments = enrollmentRepository
                    .findByOfferingIdAndStatus(offering.getId(), EnrollmentStatus.ENROLLED);
            int enrolledCount = enrollments.size();
            boolean hasProfessor = offering.getProfessorId() != null;
            boolean meetsMin = enrolledCount >= minStudents;

            String message;
            boolean committed;

            if (!hasProfessor) {
                // 备选流: No Professor for the Course Offering → 直接取消
                offering.setStatus(OfferingStatus.CANCELLED);
                offeringRepository.save(offering);
                cancelAllEnrollments(offering.getId());
                removeFromAllSchedules(offering.getId());
                cancelledForNoProfessor++;
                message = "无教授指派,已 CANCELLED 并从所有课表移除";
                committed = false;
                log.warn("开设 {} ({}) 无教授指派,已取消", offering.getId(), offering.getCourseId());
            } else if (meetsMin) {
                // 满足条件: 有教授 + 人数 ≥ 3 → COMMITTED
                offering.setStatus(OfferingStatus.COMMITTED);
                offeringRepository.save(offering);
                billableEnrollments.addAll(enrollments);
                committedCount++;
                message = String.format("有教授且满足最小人数 (%d/%d),已 COMMITTED",
                        enrolledCount, minStudents);
                committed = true;
                log.info("开设 {} ({}) 满足条件 ({}/{}),已 COMMITTED",
                        offering.getId(), offering.getCourseId(),
                        enrolledCount, minStudents);
            } else {
                // 不满足最小人数 → 暂时不处理(leveling 可能补充人数)
                // 先标记为待定,leveling 后再检查
                message = String.format("人数不足 (%d/%d),待 leveling 后重新检查",
                        enrolledCount, minStudents);
                committed = false;
                log.info("开设 {} ({}) 人数不足 ({}/{}),等待 leveling",
                        offering.getId(), offering.getCourseId(),
                        enrolledCount, minStudents);
            }

            offeringStatuses.add(new CloseRegistrationResult.OfferingStatus(
                    offering.getId(),
                    offering.getCourseId(),
                    enrolledCount,
                    minStudents,
                    committed,
                    message
            ));
        }

        // ========== 步骤 3: Leveling - 为学生课表补充备选课程 ==========
        log.info("开始 Leveling: 为学生课表补充备选课程");
        List<Schedule> allSchedules = scheduleRepository.findBySemesterAndState(
                semester, ScheduleState.DRAFT);
        int leveledCount = 0;

        for (Schedule schedule : allSchedules) {
            if (schedule.getState() == ScheduleState.CLOSED) {
                continue; // 已关闭的课表跳过
            }

            List<ScheduleItem> items = itemRepository.findByScheduleId(schedule.getId());
            long primaryEnrolled = items.stream()
                    .filter(i -> i.getKind() == ItemKind.PRIMARY
                            && i.getStatus() == ItemStatus.ENROLLED_IN)
                    .count();

            if (primaryEnrolled >= maxPrimary) {
                continue; // 主选已满,无需补充
            }

            // 获取备选列表(按添加顺序)
            List<ScheduleItem> alternates = items.stream()
                    .filter(i -> i.getKind() == ItemKind.ALTERNATE
                            && i.getStatus() == ItemStatus.SELECTED)
                    .toList();

            for (ScheduleItem alt : alternates) {
                if (primaryEnrolled >= maxPrimary) break;

                CourseOffering offering = offeringRepository.findById(alt.getOfferingId())
                        .orElse(null);
                if (offering == null) continue;

                // 检查开设是否仍 OPEN 且有容量
                if (offering.getStatus() != OfferingStatus.OPEN) continue;
                long currentEnrolled = enrollmentRepository.countByOfferingIdAndStatus(
                        offering.getId(), EnrollmentStatus.ENROLLED);
                if (currentEnrolled >= maxStudents) continue;

                // 检查学生是否已注册该开设(防重复)
                boolean alreadyEnrolled = items.stream()
                        .anyMatch(i -> i.getOfferingId().equals(alt.getOfferingId())
                                && i.getStatus() == ItemStatus.ENROLLED_IN);
                if (alreadyEnrolled) continue;

                // 将备选提升为主选并注册
                alt.setKind(ItemKind.PRIMARY);
                alt.setStatus(ItemStatus.ENROLLED_IN);
                itemRepository.save(alt);

                Enrollment enrollment = new Enrollment();
                enrollment.setOfferingId(offering.getId());
                enrollment.setStudentId(schedule.getStudentId());
                enrollment.setStatus(EnrollmentStatus.ENROLLED);
                enrollmentRepository.save(enrollment);

                // 同时将该开设加入 billableEnrollments(如果尚未包含)
                if (billableEnrollments.stream().noneMatch(e ->
                        e.getOfferingId().equals(offering.getId())
                                && e.getStudentId().equals(schedule.getStudentId()))) {
                    billableEnrollments.add(enrollment);
                }

                primaryEnrolled++;
                leveledCount++;
                log.info("Leveling: 学生 {} 备选 {} 提升为主选并注册",
                        schedule.getStudentId(), offering.getCourseId());
            }
        }
        log.info("Leveling 完成: {} 个备选被提升为主选", leveledCount);

        // ========== 步骤 4: 二次检查 - 取消仍不足最小人数的开设 ==========
        log.info("二次检查: 取消仍不足最小人数的开设");
        List<CourseOffering> recheckOfferings = offeringRepository
                .findBySemesterAndStatus(semester, OfferingStatus.OPEN);
        int finalCancelledCount = 0;

        for (CourseOffering offering : recheckOfferings) {
            long enrolledCount = enrollmentRepository.countByOfferingIdAndStatus(
                    offering.getId(), EnrollmentStatus.ENROLLED);

            if (enrolledCount < minStudents) {
                // 仍不足最小人数 → CANCELLED
                offering.setStatus(OfferingStatus.CANCELLED);
                offeringRepository.save(offering);
                cancelAllEnrollments(offering.getId());
                removeFromAllSchedules(offering.getId());
                finalCancelledCount++;

                // Update the offeringStatus entry (records are immutable, so rebuild)
                int idx = -1;
                for (int i = 0; i < offeringStatuses.size(); i++) {
                    if (offeringStatuses.get(i).getOfferingId().equals(offering.getId())) {
                        idx = i;
                        break;
                    }
                }
                if (idx >= 0) {
                    offeringStatuses.set(idx, new CloseRegistrationResult.OfferingStatus(
                        offering.getId(),
                        offering.getCourseId(),
                        (int) enrolledCount,
                        minStudents,
                        false,
                        "仍不足最小人数 (" + enrolledCount + "/" + minStudents + "),已 CANCELLED"
                    ));
                }

                log.warn("二次检查: 开设 {} ({}) 仍不足最小人数 ({}/{}),已取消",
                        offering.getId(), offering.getCourseId(),
                        enrolledCount, minStudents);
            } else {
                // leveling 后满足条件 → COMMITTED
                offering.setStatus(OfferingStatus.COMMITTED);
                offeringRepository.save(offering);

                // 更新 billableEnrollments
                List<Enrollment> enrollments = enrollmentRepository
                        .findByOfferingIdAndStatus(offering.getId(), EnrollmentStatus.ENROLLED);
                for (Enrollment e : enrollments) {
                    if (billableEnrollments.stream().noneMatch(b ->
                            b.getOfferingId().equals(e.getOfferingId())
                                    && b.getStudentId().equals(e.getStudentId()))) {
                        billableEnrollments.add(e);
                    }
                }

                // 更新 offeringStatus
                log.info("二次检查: 开设 {} ({}) leveling 后满足条件 ({}/{}),已 COMMITTED",
                        offering.getId(), offering.getCourseId(),
                        enrolledCount, minStudents);
            }
        }

        // 更新 offeringStatus 列表(反映最终状态)
        List<CloseRegistrationResult.OfferingStatus> finalStatuses = offeringStatuses.stream()
                .map(s -> {
                    CourseOffering o = offeringRepository.findById(s.getOfferingId()).orElse(null);
                    if (o != null) {
                        long count = enrollmentRepository.countByOfferingIdAndStatus(
                                o.getId(), EnrollmentStatus.ENROLLED);
                        boolean committed = o.getStatus() == OfferingStatus.COMMITTED;
                        return new CloseRegistrationResult.OfferingStatus(
                                o.getId(),
                                o.getCourseId(),
                                (int) count,
                                minStudents,
                                committed,
                                committed ? "已 COMMITTED" : "已 CANCELLED"
                        );
                    }
                    return s;
                })
                .toList();

        // ========== 步骤 5: 通知 Billing 系统 ==========
        boolean billingNotificationSent = notifyBillingSystem(billableEnrollments, semester);

        // ========== 步骤 6: 通知所有学生注册已关闭 ==========
        notifyAllStudentsRegistrationClosed(semester);

        // 统计最终结果
        committedCount = (int) finalStatuses.stream().filter(s -> s.isMeetsMinimum()).count();
        int cancelledCount = finalStatuses.size() - committedCount;

        String message = String.format(
                "注册已关闭。共 %d 门开设: %d 门已 COMMITTED, %d 门已 CANCELLED。"
                        + "Leveling 提升 %d 个备选为主选。",
                allOfferings.size(), committedCount, cancelledCount, leveledCount);

        if (!billingNotificationSent) {
            message += " 警告: 通知 Billing 系统失败,请手动处理。";
        }

        log.info("===== 关闭注册完成: {} =====", message);

        return new CloseRegistrationResult(
                true,
                semester,
                message,
                allOfferings.size(),
                committedCount,
                cancelledCount,
                finalStatuses,
                billingNotificationSent
        );
    }

    /**
     * 获取某学期的注册统计(关闭前预览)。
     */
    @Transactional(readOnly = true)
    public CloseRegistrationResult previewCloseRegistration(String semester) {
        List<CourseOffering> offerings = offeringRepository
                .findBySemesterAndStatus(semester, OfferingStatus.OPEN);

        List<CloseRegistrationResult.OfferingStatus> offeringStatuses = new ArrayList<>();
        int fullCount = 0;
        int underFilledCount = 0;

        for (CourseOffering offering : offerings) {
            long enrolledCount = enrollmentRepository.countByOfferingIdAndStatus(
                    offering.getId(), EnrollmentStatus.ENROLLED);
            boolean hasProfessor = offering.getProfessorId() != null;
            boolean meetsMin = enrolledCount >= minStudents && hasProfessor;
            if (meetsMin) fullCount++;
            else underFilledCount++;

            String msg;
            if (!hasProfessor) {
                msg = "无教授指派,将被取消";
            } else if (enrolledCount < minStudents) {
                msg = String.format("未达最小人数 (%d/%d),将被取消", enrolledCount, minStudents);
            } else {
                msg = "满足要求,将被 COMMITTED";
            }

            offeringStatuses.add(new CloseRegistrationResult.OfferingStatus(
                    offering.getId(),
                    offering.getCourseId(),
                    (int) enrolledCount,
                    minStudents,
                    meetsMin,
                    msg
            ));
        }

        return new CloseRegistrationResult(
                true,
                semester,
                String.format("预览: %d 门开设, %d 满足, %d 不满足",
                        offerings.size(), fullCount, underFilledCount),
                offerings.size(),
                fullCount,
                underFilledCount,
                offeringStatuses,
                false
        );
    }

    // ==================== 内部辅助方法 ====================

    /** 取消某开设的所有选课记录并通知学生 */
    private void cancelAllEnrollments(Long offeringId) {
        CourseOffering offering = offeringRepository.findById(offeringId).orElse(null);
        List<Enrollment> enrollments = enrollmentRepository
                .findByOfferingIdAndStatus(offeringId, EnrollmentStatus.ENROLLED);
        for (Enrollment e : enrollments) {
            e.setStatus(EnrollmentStatus.CANCELLED);
            enrollmentRepository.save(e);

            // 通知学生课程已取消
            if (offering != null) {
                notificationService.notifyCourseCancelled(offeringId);
            }
        }
        log.info("已取消开设 {} 的 {} 条选课记录并通知学生", offeringId, enrollments.size());
    }

    /** 从所有课表中移除某开设(对应 Delete a Schedule 中的 Roster 移除逻辑) */
    private void removeFromAllSchedules(Long offeringId) {
        List<ScheduleItem> items = itemRepository.findByOfferingId(offeringId);
        for (ScheduleItem item : items) {
            if (item.getStatus() == ItemStatus.ENROLLED_IN
                    || item.getStatus() == ItemStatus.SELECTED) {
                item.setStatus(ItemStatus.CANCELLED);
                itemRepository.save(item);
                log.info("从课表 {} 移除开设 {}", item.getScheduleId(), offeringId);
            }
        }
    }

    /**
     * 通知 Billing 系统(模拟实现)。
     * <p>
     * 实际应通过 HTTP/RPC 调用 Billing 系统的接口,将成功开设的选课名单发送过去。
     * 备选流 Billing System Down: 实际实现应捕获异常并返回 false。
     */
    private boolean notifyBillingSystem(List<Enrollment> enrollments, String semester) {
        try {
            log.info("通知 Billing 系统: semester={}, 共 {} 条选课记录",
                    semester, enrollments.size());

            // 按学生分组统计
            Map<Long, Long> studentCourseCount = enrollments.stream()
                    .collect(Collectors.groupingBy(
                            Enrollment::getStudentId,
                            Collectors.counting()));

            log.info("Billing 通知: {} 名学生, 选课记录总计 {} 条",
                    studentCourseCount.size(), enrollments.size());

            // 模拟: 实际应调用 BillingClient.notify(semester, enrollments)
            return true;
        } catch (Exception e) {
            log.error("通知 Billing 系统失败", e);
            return false;
        }
    }

    /** 通知所有本学期有选课记录的学生注册已关闭 */
    private void notifyAllStudentsRegistrationClosed(String semester) {
        List<CourseOffering> openOfferings = offeringRepository
                .findBySemesterAndStatus(semester, OfferingStatus.OPEN);
        List<CourseOffering> committedOfferings = offeringRepository
                .findBySemesterAndStatus(semester, OfferingStatus.COMMITTED);
        List<CourseOffering> allOfferings = new ArrayList<>(openOfferings);
        allOfferings.addAll(committedOfferings);

        java.util.Set<Long> notifiedStudents = new java.util.HashSet<>();
        for (CourseOffering offering : allOfferings) {
            List<Enrollment> enrollments = enrollmentRepository
                    .findByOfferingIdAndStatus(offering.getId(), EnrollmentStatus.ENROLLED);
            for (Enrollment e : enrollments) {
                if (notifiedStudents.add(e.getStudentId())) {
                    notificationService.notifyRegistrationClosed(e.getStudentId(), semester);
                }
            }
        }
        log.info("已通知 {} 名学生: 注册已关闭", notifiedStudents.size());
    }
}
