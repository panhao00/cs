package com.crs.repository;

import com.crs.domain.Waitlist;
import com.crs.domain.WaitlistStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 候补名单仓储。
 */
@Repository
public interface WaitlistRepository extends JpaRepository<Waitlist, Long> {

    /** 查看某学生在某开设上的候补记录(用于去重检查) */
    Optional<Waitlist> findByOfferingIdAndStudentIdAndStatus(
            Long offeringId, Long studentId, WaitlistStatus status);

    /** 查看某学生的所有候补记录(按创建时间排序) */
    List<Waitlist> findByStudentIdAndStatusOrderByCreatedAtAsc(
            Long studentId, WaitlistStatus status);

    /** 查看某开设的所有候补记录(按 position 排序) */
    List<Waitlist> findByOfferingIdAndStatusOrderByPositionAsc(
            Long offeringId, WaitlistStatus status);

    /** 获取某开设的最大 position(用于自动递增) */
    @Query("SELECT COALESCE(MAX(w.position), 0) FROM Waitlist w WHERE w.offeringId = :offeringId")
    int findMaxPositionByOfferingId(@Param("offeringId") Long offeringId);
}
