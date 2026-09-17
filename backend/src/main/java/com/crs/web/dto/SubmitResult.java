package com.crs.web.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Submit Schedule 结果 DTO。
 * <p>
 * 校验失败的开设会列出在 errors 中,学生可据此调整课表。
 * 若课程在构建期间被填满,errors 中会标记 "CAPACITY_FULL" 类型。
 */
@Getter
@Setter
@AllArgsConstructor
public class SubmitResult {
    private boolean success;
    private String message;
    private List<EnrollmentResult> results;
    private List<ValidationError> errors;

    @Getter
    @Setter
    @AllArgsConstructor
    public static class EnrollmentResult {
        private Long offeringId;
        private String courseId;
        private boolean enrolled;
        private String message;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class ValidationError {
        private Long offeringId;
        private String courseId;
        private String errorType;  // PREREQUISITE_NOT_MET, OFFERING_NOT_OPEN, CAPACITY_FULL, TIME_CONFLICT
        private String message;
    }
}
