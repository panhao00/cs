package com.crs.web;

import com.crs.service.AuditLogQueryService;
import com.crs.web.dto.AuditLogDTO;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 审计日志查询控制器: 注册主任查看系统操作审计日志。
 * <p>
 * 端点:
 * - GET /audit-logs      查询审计日志(支持分页和过滤)
 * - GET /audit-logs/stats 获取审计日志统计
 * <p>
 * 安全: 强制 REGISTRAR 角色。
 */
@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogQueryService auditLogQueryService;
    private final RoleGuard roleGuard;

    /**
     * 查询审计日志(支持按操作类型和操作者 ID 过滤)。
     *
     * @param action  操作类型过滤(可选)
     * @param actorId 操作者 ID 过滤(可选)
     * @param page    页码(默认 0)
     * @param size    每页大小(默认 20)
     * @return 分页审计日志
     */
    @GetMapping
    public ResponseEntity<Page<AuditLogDTO>> getAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long actorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        roleGuard.requireRegistrar();
        return ResponseEntity.ok(auditLogQueryService.getAuditLogs(action, actorId, page, size));
    }

    /**
     * 获取审计日志统计信息。
     * <p>
     * 返回总数和各操作类型的计数。
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAuditLogStats() {
        roleGuard.requireRegistrar();
        return ResponseEntity.ok(auditLogQueryService.getAuditLogStats());
    }
}
