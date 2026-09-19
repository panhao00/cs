package com.crs.web.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 课程评价响应 DTO。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDTO {
    private Long id;
    private Long offeringId;
    private String courseId;
    private String courseTitle;
    private Long studentId;
    private String studentName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
