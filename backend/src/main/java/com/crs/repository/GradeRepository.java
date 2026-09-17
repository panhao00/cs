package com.crs.repository;

import com.crs.domain.Grade;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 成绩仓储。
 */
@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {

    Optional<Grade> findByOfferingIdAndStudentId(Long offeringId, Long studentId);

    List<Grade> findByStudentId(Long studentId);

    List<Grade> findByOfferingId(Long offeringId);
}
