package com.crs.service;

import com.crs.common.ApiException;
import com.crs.common.RegistrationPhase;
import com.crs.common.TimeConflictUtil;
import com.crs.domain.Course;
import com.crs.domain.CourseOffering;
import com.crs.domain.EnrollmentStatus;
import com.crs.domain.OfferingStatus;
import com.crs.repository.CourseOfferingRepository;
import com.crs.repository.CourseRepository;
import com.crs.repository.EnrollmentRepository;
import com.crs.web.dto.TeachingOfferingDTO;
import com.crs.web.dto.TeachingSelectionResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 教授课课服务: 实现用例 7.3 Select Courses to Teach。
 * <p>
 * 基本流:
 * 1. 系统显示教授本学期有资格授课的开设列表,以及教授已选授课列表
 * 2. 教授选择/取消选择下学期欲授课程
 * 3. 系统移除被取消开设上的教授指派
 * 4. 系统校验所选开设之间及与已选授课之间无时间冲突;无冲突则更新各开设的授课教授
 * <p>
 * 备选流:
 * - No Course Offerings Available: 无资格授课 → 报错,用例终止
 * - Schedule Conflict: 时间冲突 → 报错并指出冲突课程
 * - Course Registration Closed: 注册关闭 → 提示并终止
 * <p>
 * 参考 AI 生成,经人工核验与调整。时间冲突检测算法由人工设计。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeachingService {

    private final CourseOfferingRepository offeringRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final RegistrationPhase registrationPhase;
    private final AuditService auditService;

    /**
     * 查询教授本学期有资格授课的开设列表 + 已选授课标记。
     */
    @Transactional(readOnly = true)
    public List<TeachingOfferingDTO> getEligibleOfferings(Long professorId, String semester) {
        List<CourseOffering> offerings = offeringRepository
                .findBySemesterAndStatus(semester, OfferingStatus.OPEN);

        if (offerings.isEmpty()) {
            log.info("教授 {} 在 {} 学期无可用开设", professorId, semester);
            return List.of();
        }

        return offerings.stream()
                .map(o -> toDTO(o, professorId))
                .toList();
    }

    /**
     * 选择/取消授课课程(基本流 2-4)。
     * <p>
     * 教授提交全量欲授课程列表,系统据此:
     * - 移除不在列表中的已有指派
     * - 校验所选开设之间无时间冲突
     * - 无冲突则更新各开设的 professorId
     */
    @Transactional
    public TeachingSelectionResult selectCoursesToTeach(
            Long professorId, String semester, List<Long> desiredOfferingIds) {
        assertRegistrationOpen();

        // 获取本学期所有 OPEN 开设
        List<CourseOffering> allOpen = offeringRepository
                .findBySemesterAndStatus(semester, OfferingStatus.OPEN);
        Map<Long, CourseOffering> offeringMap = new HashMap<>();
        for (CourseOffering o : allOpen) {
            offeringMap.put(o.getId(), o);
        }

        // 当前已选(教授 ID == 当前教授)
        Set<Long> currentlySelected = new HashSet<>();
        for (CourseOffering o : allOpen) {
            if (professorId.equals(o.getProfessorId())) {
                currentlySelected.add(o.getId());
            }
        }

        // 验证所有 desiredOfferingId 都存在且开放
        List<CourseOffering> desired = new ArrayList<>();
        for (Long id : desiredOfferingIds) {
            CourseOffering o = offeringMap.get(id);
            if (o == null) {
                throw ApiException.badRequest("课程开设 " + id + " 不存在或未开放");
            }
            desired.add(o);
        }

        // 时间冲突检测(所选开设之间)
        List<TeachingSelectionResult.ConflictInfo> conflicts = detectTimeConflicts(desired);

        if (!conflicts.isEmpty()) {
            log.info("教授 {} 选课存在 {} 个时间冲突", professorId, conflicts.size());
            return new TeachingSelectionResult(
                    false,
                    "存在时间冲突,请调整后重试",
                    desired.stream().map(o -> toDTO(o, professorId)).toList(),
                    conflicts
            );
        }

        // 移除被取消的开设上的教授指派(基本流 3)
        for (CourseOffering o : allOpen) {
            if (professorId.equals(o.getProfessorId())
                    && !desiredOfferingIds.contains(o.getId())) {
                o.setProfessorId(null);
                offeringRepository.save(o);
                log.info("教授 {} 取消授课开设 {}", professorId, o.getId());
            }
        }

        // 指派新选择的开设(基本流 4)
        for (CourseOffering o : desired) {
            if (!professorId.equals(o.getProfessorId())) {
                // 水平越权防护: 不能接管其他教授已指派的开设
                if (o.getProfessorId() != null && !professorId.equals(o.getProfessorId())) {
                    throw ApiException.forbidden(
                            "课程开设 " + o.getId() + " 已被其他教授选择,无法接管");
                }
                o.setProfessorId(professorId);
                offeringRepository.save(o);
                log.info("教授 {} 选择授课开设 {} ({})", professorId, o.getId(), o.getCourseId());
            }
        }

        List<TeachingOfferingDTO> selected = desired.stream()
                .map(o -> toDTO(o, professorId))
                .toList();

        auditService.recordTeachingSelection(professorId, semester,
                desiredOfferingIds.size(), true);

        return new TeachingSelectionResult(true, "授课选择已更新", selected, List.of());
    }

    // ---- 内部方法 ----

    /**
     * 教授授课选择仅在 open 阶段允许。
     * add-drop 阶段教授不能再修改授课安排。
     */
    private void assertRegistrationOpen() {
        String phase = registrationPhase.getPhase();
        if (!"open".equalsIgnoreCase(phase)) {
            String msg = "add-drop".equalsIgnoreCase(phase)
                    ? "当前为增减课阶段,教授不能修改授课安排"
                    : "注册已关闭,教授不能修改授课安排";
            throw ApiException.conflict(msg);
        }
    }

    /**
     * 时间冲突检测: 同一天时间段重叠即视为冲突。
     * days 如 "MWF" / "TR",times 如 "09:00-10:00"。
     * 简化算法: days+times 相同则冲突(严格匹配)。实际可解析时间段做更精细比较。
     */
    private List<TeachingSelectionResult.ConflictInfo> detectTimeConflicts(
            List<CourseOffering> offerings) {
        List<TeachingSelectionResult.ConflictInfo> conflicts = new ArrayList<>();
        for (int i = 0; i < offerings.size(); i++) {
            for (int j = i + 1; j < offerings.size(); j++) {
                CourseOffering a = offerings.get(i);
                CourseOffering b = offerings.get(j);
                if (hasTimeOverlap(a, b)) {
                    conflicts.add(new TeachingSelectionResult.ConflictInfo(
                            a.getId(), a.getCourseId(),
                            b.getId(), b.getCourseId(),
                            a.getDays(), a.getTimes(),
                            "课程 " + a.getCourseId() + " 与 " + b.getCourseId()
                                    + " 时间冲突 (" + a.getDays() + " " + a.getTimes() + ")"
                    ));
                }
            }
        }
        return conflicts;
    }

    /** 两个开设时间是否重叠(委托给 TimeConflictUtil) */
    private boolean hasTimeOverlap(CourseOffering a, CourseOffering b) {
        if (a.getDays() == null || b.getDays() == null) return false;
        if (a.getTimes() == null || b.getTimes() == null) return false;
        return TimeConflictUtil.isConflict(a.getDays(), a.getTimes(), b.getDays(), b.getTimes());
    }

    private TeachingOfferingDTO toDTO(CourseOffering o, Long professorId) {
        Course c = courseRepository.findById(o.getCourseId()).orElse(null);
        long enrolled = enrollmentRepository.countByOfferingIdAndStatus(
                o.getId(), EnrollmentStatus.ENROLLED);
        return new TeachingOfferingDTO(
                o.getId(),
                o.getCourseId(),
                c != null ? c.getTitle() : null,
                c != null ? c.getDepartment() : null,
                o.getSemester(),
                o.getDays(),
                o.getTimes(),
                o.getRoom(),
                o.getStatus(),
                professorId.equals(o.getProfessorId()),
                (int) enrolled,
                o.getMaxCapacity()
        );
    }
}
