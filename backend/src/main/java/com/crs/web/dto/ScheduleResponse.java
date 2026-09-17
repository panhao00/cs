package com.crs.web.dto;

import com.crs.domain.ItemKind;
import com.crs.domain.ItemStatus;
import com.crs.domain.ScheduleState;
import java.util.ArrayList;
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

    /** 容量警告列表: 学生构建课表期间,若课程被填满则提前通知 */
    private List<CapacityWarning> capacityWarnings;

    /** 无参构造(兼容旧调用) */
    public ScheduleResponse(Long scheduleId, Long studentId, String semester,
                            ScheduleState state, List<ScheduleItemDTO> items,
                            int primaryCount, int alternateCount) {
        this(scheduleId, studentId, semester, state, items, primaryCount, alternateCount,
                new ArrayList<>());
    }

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
