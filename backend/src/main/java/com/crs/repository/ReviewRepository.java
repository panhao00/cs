package com.crs.repository;

import com.crs.domain.Review;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 课程评价仓储。
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /** 查询某开设的所有评价 */
    List<Review> findByOfferingIdOrderByCreatedAtDesc(Long offeringId);

    /** 查询某学生对某开设的评价(用于唯一性检查) */
    Optional<Review> findByOfferingIdAndStudentId(Long offeringId, Long studentId);

    /** 查询某学生的所有评价 */
    List<Review> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    /** 计算某开设的平均评分 */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.offeringId = :offeringId")
    Double findAverageRatingByOfferingId(@Param("offeringId") Long offeringId);

    /** 计算某课程所有开设的综合平均评分 */
    @Query("SELECT AVG(r.rating) FROM Review r "
            + "JOIN CourseOffering o ON r.offeringId = o.id "
            + "WHERE o.courseId = :courseId")
    Double findAverageRatingByCourseId(@Param("courseId") String courseId);

    /** 检查某学生是否已对某开设评价过 */
    boolean existsByOfferingIdAndStudentId(Long offeringId, Long studentId);
}
