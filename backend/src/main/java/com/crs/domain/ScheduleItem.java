package com.crs.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 课表项(课表中的一门课程开设)。
 * kind:   PRIMARY(主选) / ALTERNATE(备选)
 * status: SELECTED(已选) / ENROLLED_IN(已注册) / CANCELLED(已取消)
 */
@Entity
@Table(name = "schedule_item")
@Getter
@Setter
public class ScheduleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "schedule_id", nullable = false)
    private Long scheduleId;

    @Column(name = "offering_id", nullable = false)
    private Long offeringId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemKind kind;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ItemStatus status = ItemStatus.SELECTED;
}
