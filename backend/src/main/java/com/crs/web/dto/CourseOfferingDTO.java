package com.crs.web.dto;

import com.crs.domain.OfferingStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 课程开设视图 DTO(展示给学生的可选课程列表)。
 */
@Getter
@Setter
@AllArgsConstructor
public class CourseOfferingDTO {
    private Long offeringId;
    private String courseId;
    private String courseTitle;
    private String department;
    private Integer credits;
    private String prerequisiteId;
    private String semester;
    private String days;
    private String times;
    private String room;
    private int maxCapacity;
    private int enrolledCount;
    private int remainingSeats;
    private OfferingStatus status;
    private Long professorId;
    private String professorName;
}
