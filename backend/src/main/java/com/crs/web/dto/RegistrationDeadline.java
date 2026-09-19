package com.crs.web.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 注册时间节点 DTO。
 * 描述一个选课相关的重要日期节点及其状态。
 */
@Getter
@Setter
@AllArgsConstructor
public class RegistrationDeadline {

    /** 节点名称 */
    private String name;

    /** 节点日期时间 */
    private LocalDateTime date;

    /** 状态: PASSED / ACTIVE / UPCOMING */
    private DeadlineStatus status;

    /** 距今剩余天数(已过则为 0) */
    private int daysRemaining;

    /** 节点描述 */
    private String description;

    public enum DeadlineStatus {
        /** 已过 */
        PASSED,
        /** 进行中(今天) */
        ACTIVE,
        /** 即将到来 */
        UPCOMING
    }
}
