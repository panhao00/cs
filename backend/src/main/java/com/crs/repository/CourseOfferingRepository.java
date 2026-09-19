package com.crs.repository;

import com.crs.domain.CourseOffering;
import com.crs.domain.OfferingStatus;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 课程开设仓储。
 */
@Repository
public interface CourseOfferingRepository extends JpaRepository<CourseOffering, Long> {

    List<CourseOffering> findBySemesterAndStatus(String semester, OfferingStatus status);

    List<CourseOffering> findBySemesterAndProfessorId(String semester, Long professorId);

    List<CourseOffering> findByCourseIdAndSemester(String courseId, String semester);

    /** 以悲观写锁查询开设,用于并发选课时的容量校验。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM CourseOffering o WHERE o.id = :id")
    Optional<CourseOffering> findByIdWithPessimisticLock(@Param("id") Long id);

    /** 批量查询开设,减少 N+1 查询 */
    @Query("SELECT o FROM CourseOffering o WHERE o.id IN :ids")
    List<CourseOffering> findByIdIn(@Param("ids") List<Long> ids);
}
