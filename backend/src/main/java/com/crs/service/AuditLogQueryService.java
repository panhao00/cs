package com.crs.service;

import com.crs.domain.AuditLog;
import com.crs.domain.User;
import com.crs.repository.AuditLogRepository;
import com.crs.repository.UserRepository;
import com.crs.web.dto.AuditLogDTO;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 审计日志查询服务: 提供审计日志的分页查询和统计功能。
 * <p>
 * 用于注册主任查看系统操作审计日志,
 * 支持按操作类型和操作者 ID 过滤。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogQueryService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    /**
     * 查询审计日志(支持按操作类型和操作者 ID 过滤)。
     *
     * @param action  操作类型过滤(可为 null,表示不过滤)
     * @param actorId 操作者 ID 过滤(可为 null,表示不过滤)
     * @param page    页码(从 0 开始)
     * @param size    每页大小
     * @return 分页审计日志 DTO
     */
    @Transactional(readOnly = true)
    public Page<AuditLogDTO> getAuditLogs(String action, Long actorId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<AuditLog> auditLogs;

        if (action != null && actorId != null) {
            auditLogs = auditLogRepository
                    .findByActionAndActorIdOrderByCreatedAtDesc(action, actorId, pageable);
        } else if (action != null) {
            auditLogs = auditLogRepository
                    .findByActionOrderByCreatedAtDesc(action, pageable);
        } else if (actorId != null) {
            auditLogs = auditLogRepository
                    .findByActorIdOrderByCreatedAtDesc(actorId, pageable);
        } else {
            auditLogs = auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        return auditLogs.map(this::toDTO);
    }

    /**
     * 获取审计日志统计信息。
     * <p>
     * 返回: total(总数), 以及各操作类型的计数。
     *
     * @return 统计信息映射
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAuditLogStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        long total = auditLogRepository.count();
        stats.put("total", total);

        // 按操作类型统计
        stats.put("LOGIN_SUCCESS", auditLogRepository.countByAction("LOGIN_SUCCESS"));
        stats.put("LOGIN_FAILED", auditLogRepository.countByAction("LOGIN_FAILED"));
        stats.put("GRADE_ENTRY", auditLogRepository.countByAction("GRADE_ENTRY"));
        stats.put("SCHEDULE_SUBMIT", auditLogRepository.countByAction("SCHEDULE_SUBMIT"));
        stats.put("SCHEDULE_DELETE", auditLogRepository.countByAction("SCHEDULE_DELETE"));
        stats.put("USER_CREATE", auditLogRepository.countByAction("USER_CREATE"));
        stats.put("USER_UPDATE", auditLogRepository.countByAction("USER_UPDATE"));
        stats.put("USER_DELETE", auditLogRepository.countByAction("USER_DELETE"));
        stats.put("REPORT_CARD_VIEW", auditLogRepository.countByAction("REPORT_CARD_VIEW"));
        stats.put("REGISTRATION_CLOSE", auditLogRepository.countByAction("REGISTRATION_CLOSE"));
        stats.put("TEACHING_SELECT", auditLogRepository.countByAction("TEACHING_SELECT"));
        stats.put("GRADE_EXPORT", auditLogRepository.countByAction("GRADE_EXPORT"));

        return stats;
    }

    /**
     * 将 AuditLog 实体转换为 AuditLogDTO。
     * <p>
     * 将 actorId 解析为 actorName(若用户存在)。
     *
     * @param auditLog 审计日志实体
     * @return 审计日志 DTO
     */
    private AuditLogDTO toDTO(AuditLog auditLog) {
        String actorName = null;
        if (auditLog.getActorId() != null) {
            Optional<User> user = userRepository.findById(auditLog.getActorId());
            actorName = user.map(User::getName).orElse("Unknown");
        }
        return new AuditLogDTO(
                auditLog.getId(),
                auditLog.getActorId(),
                actorName,
                auditLog.getAction(),
                auditLog.getTargetType(),
                auditLog.getTargetId(),
                auditLog.getDetails(),
                auditLog.getCreatedAt()
        );
    }
}
