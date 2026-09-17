package com.crs.repository;

import com.crs.domain.Enrollment;
import com.crs.domain.EnrollmentStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 选课记录仓储(用于班级名单 Roster)。
 */
@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    long countByOfferingIdAndStatus(Long offeringId, EnrollmentStatus status);

    List<Enrollment> findByOfferingIdAndStatus(Long offeringId, EnrollmentStatus status);

    List<Enrollment> findByStudentIdAndStatus(Long studentId, EnrollmentStatus status);

    /** 按学生+学期+状态查询选课记录(用于 ReportCard,避免全表扫描) */
    @Query("SELECT e FROM Enrollment e JOIN CourseOffering o ON e.offeringId = o.id "
            + "WHERE e.studentId = :studentId AND o.semester = :semester AND e.status = :status")
    List<Enrollment> findByStudentIdAndSemesterAndStatus(
            @Param("studentId") Long studentId,
            @Param("semester") String semester,
            @Param("status") EnrollmentStatus status);
}
