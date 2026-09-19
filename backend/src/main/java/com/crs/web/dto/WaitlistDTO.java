package com.crs.web.dto;

import com.crs.domain.WaitlistStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 候补名单响应 DTO。
 */
@Getter
@Setter
@AllArgsConstructor
public class WaitlistDTO {
    private Long id;
    private Long offeringId;
    private String courseId;
    private String courseTitle;
    private Long studentId;
    private String studentName;
    private int position;
    private WaitlistStatus status;
    private LocalDateTime createdAt;
}
