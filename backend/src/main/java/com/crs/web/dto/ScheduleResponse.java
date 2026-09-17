package com.crs.web.dto;

import com.crs.domain.ItemKind;
import com.crs.domain.ItemStatus;
import com.crs.domain.ScheduleState;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 课表响应 DTO(含课表项)。
 */
@Getter
@Setter
@AllArgsConstructor
public class ScheduleResponse {
    private Long scheduleId;
    private Long studentId;
    private String semester;
    private ScheduleState state;
    private List<ScheduleItemDTO> items;
    private int primaryCount;
    private int alternateCount;

    @Getter
    @Setter
    @AllArgsConstructor
    public static class ScheduleItemDTO {
        private Long itemId;
        private Long offeringId;
        private String courseId;
        private String courseTitle;
        private String days;
        private String times;
        private String room;
        private ItemKind kind;
        private ItemStatus status;
        private Long professorId;
        private String professorName;
        private int enrolledCount;
        private int maxCapacity;
    }
}
