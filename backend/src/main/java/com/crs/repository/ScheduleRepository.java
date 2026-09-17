package com.crs.repository;

import com.crs.domain.Schedule;
import com.crs.domain.ScheduleState;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 课表仓储。
 */
@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    Optional<Schedule> findByStudentIdAndSemester(Long studentId, String semester);

    List<Schedule> findBySemesterAndState(String semester, ScheduleState state);

    List<Schedule> findByStudentId(Long studentId);
}
