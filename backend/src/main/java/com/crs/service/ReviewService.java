package com.crs.service;

import com.crs.common.ApiException;
import com.crs.domain.*;
import com.crs.repository.*;
import com.crs.web.dto.ReviewDTO;
import com.crs.web.dto.ReviewSubmitRequest;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 课程评价服务。
 * <p>
 * 核心业务规则:
 * - 只有已选过该课程(Enrollment status=ENROLLED)的学生可以评价
 * - 一个学生对一个开设只能评价一次
 * - 学生只能修改/删除自己的评价
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseOfferingRepository offeringRepository;
    private final UserRepository userRepository;
    private final com.crs.adapter.CatalogAdapter catalogAdapter;

    /**
     * 提交评价。
     *
     * @throws ApiException 403 如果未选课; 409 如果已有评价; 404 如果开设不存在
     */
    @Transactional
    public ReviewDTO submitReview(Long studentId, Long offeringId, ReviewSubmitRequest request) {
        // 校验开设是否存在
        CourseOffering offering = offeringRepository.findById(offeringId)
                .orElseThrow(() -> ApiException.notFound("课程开设不存在: " + offeringId));

        // 校验学生是否已选过该课程
        verifyEnrollment(studentId, offeringId);

        // 校验是否已有评价
        if (reviewRepository.existsByOfferingIdAndStudentId(offeringId, studentId)) {
            throw ApiException.conflict("您已经评价过该课程,请使用修改功能");
        }

        Review review = new Review();
        review.setOfferingId(offeringId);
        review.setStudentId(studentId);
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        review = reviewRepository.save(review);
        log.info("学生 {} 提交了对开设 {} 的评价,评分={}", studentId, offeringId, request.getRating());

        return toDTO(review);
    }

    /**
     * 修改评价。
     *
     * @throws ApiException 403 如果不是评价所有者; 404 如果评价不存在
     */
    @Transactional
    public ReviewDTO updateReview(Long studentId, Long reviewId, ReviewSubmitRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ApiException.notFound("评价不存在: " + reviewId));

        if (!review.getStudentId().equals(studentId)) {
            throw ApiException.forbidden("只能修改自己的评价");
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment());

        review = reviewRepository.save(review);
        log.info("学生 {} 修改了评价 {}", studentId, reviewId);

        return toDTO(review);
    }

    /**
     * 删除评价。
     *
     * @throws ApiException 403 如果不是评价所有者; 404 如果评价不存在
     */
    @Transactional
    public void deleteReview(Long studentId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ApiException.notFound("评价不存在: " + reviewId));

        if (!review.getStudentId().equals(studentId)) {
            throw ApiException.forbidden("只能删除自己的评价");
        }

        reviewRepository.delete(review);
        log.info("学生 {} 删除了评价 {}", studentId, reviewId);
    }

    /**
     * 获取某开设的所有评价。
     */
    @Transactional(readOnly = true)
    public List<ReviewDTO> getOfferingReviews(Long offeringId) {
        if (!offeringRepository.existsById(offeringId)) {
            throw ApiException.notFound("课程开设不存在: " + offeringId);
        }
        return reviewRepository.findByOfferingIdOrderByCreatedAtDesc(offeringId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * 获取某开设的平均评分(保留 1 位小数)。
     */
    @Transactional(readOnly = true)
    public Optional<Double> getOfferingAverageRating(Long offeringId) {
        Double avg = reviewRepository.findAverageRatingByOfferingId(offeringId);
        if (avg == null) {
            return Optional.empty();
        }
        return Optional.of(Math.round(avg * 10.0) / 10.0);
    }

    /**
     * 获取课程所有开设的综合平均评分。
     */
    @Transactional(readOnly = true)
    public Optional<Double> getCourseAverageRating(String courseId) {
        Double avg = reviewRepository.findAverageRatingByCourseId(courseId);
        if (avg == null) {
            return Optional.empty();
        }
        return Optional.of(Math.round(avg * 10.0) / 10.0);
    }

    /**
     * 获取某学生的所有评价。
     */
    @Transactional(readOnly = true)
    public List<ReviewDTO> getMyReviews(Long studentId) {
        return reviewRepository.findByStudentIdOrderByCreatedAtDesc(studentId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    // ---- 内部方法 ----

    /** 校验学生是否已选过该课程(Enrollment status=ENROLLED) */
    private void verifyEnrollment(Long studentId, Long offeringId) {
        boolean enrolled = enrollmentRepository
                .findByOfferingIdAndStatus(offeringId, EnrollmentStatus.ENROLLED)
                .stream()
                .anyMatch(e -> e.getStudentId().equals(studentId));
        if (!enrolled) {
            throw ApiException.forbidden("只有已选过该课程的学生才能评价");
        }
    }

    private ReviewDTO toDTO(Review review) {
        CourseOffering offering = offeringRepository.findById(review.getOfferingId()).orElse(null);
        String courseId = offering != null ? offering.getCourseId() : null;
        String courseTitle = null;
        if (courseId != null) {
            courseTitle = catalogAdapter.getCourse(courseId)
                    .map(Course::getTitle)
                    .orElse(null);
        }
        String studentName = userRepository.findById(review.getStudentId())
                .map(User::getName)
                .orElse(null);

        return new ReviewDTO(
                review.getId(),
                review.getOfferingId(),
                courseId,
                courseTitle,
                review.getStudentId(),
                studentName,
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}
