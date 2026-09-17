package com.crs.repository;

import com.crs.domain.ScheduleItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 课表项仓储。
 */
@Repository
public interface ScheduleItemRepository extends JpaRepository<ScheduleItem, Long> {

    List<ScheduleItem> findByScheduleId(Long scheduleId);

    List<ScheduleItem> findByOfferingId(Long offeringId);
}
