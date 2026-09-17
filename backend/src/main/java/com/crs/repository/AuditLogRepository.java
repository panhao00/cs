package com.crs.repository;

import com.crs.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 审计日志仓储。
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
