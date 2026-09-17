package com.crs.web.dto;

import com.crs.domain.OfferingStatus;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 教授授课选择结果(含时间冲突报告)。
 */
@Getter
@Setter
@AllArgsConstructor
public class TeachingSelectionResult {
    private boolean success;
    private String message;
    private List<TeachingOfferingDTO> selected;
    /** 时间冲突详情 */
    private List<ConflictInfo> conflicts;

    @Getter
    @Setter
    @AllArgsConstructor
    public static class ConflictInfo {
        private Long offeringId1;
        private String courseId1;
        private Long offeringId2;
        private String courseId2;
        private String days;
        private String times;
        private String message;
    }
}
