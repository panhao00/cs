package com.crs.repository;

import com.crs.domain.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 通知仓储。
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    List<Notification> findByStudentIdAndReadFalseOrderByCreatedAtDesc(Long studentId);

    long countByStudentIdAndReadFalse(Long studentId);
}
