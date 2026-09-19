package com.crs.repository;

import com.crs.domain.Bill;
import com.crs.domain.BillStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 账单仓储。
 */
@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    List<Bill> findBySemesterAndStatus(String semester, BillStatus status);

    List<Bill> findByStudentId(Long studentId);

    List<Bill> findByStatusAndNextRetryAtBefore(BillStatus status, LocalDateTime dateTime);

    boolean existsByStudentIdAndStatus(Long studentId, BillStatus status);
}
