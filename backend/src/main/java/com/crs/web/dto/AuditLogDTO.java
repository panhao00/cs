package com.crs.web.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 审计日志传输对象(DTO): 用于审计日志查询的安全增强传输。
 * <p>
 * 将 actorId 解析为 actorName,提供更友好的查询结果。
 */
@Getter
@Setter
@AllArgsConstructor
public class AuditLogDTO {
    private Long id;
    private Long actorId;
    private String actorName;
    private String action;
    private String targetType;
    private Long targetId;
    private String details;
    private LocalDateTime createdAt;
}
