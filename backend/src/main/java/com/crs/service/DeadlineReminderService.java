package com.crs.service;

import com.crs.web.dto.RegistrationDeadline;
import com.crs.web.dto.RegistrationDeadline.DeadlineStatus;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 选课倒计时提醒服务。
 * <p>
 * 根据学期名称,返回当前学期的重要时间节点列表及剩余天数。
 * <p>
 * 时间节点:
 * <ul>
 *   <li>注册开始时间(学期首日前三周的周一)</li>
 *   <li>增减课截止时间(注册开始后 2 周)</li>
 *   <li>注册关闭时间(注册开始后 4 周)</li>
 *   <li>成绩提交截止时间(学期结束前 1 周)</li>
 * </ul>
 */
@Slf4j
@Service
public class DeadlineReminderService {

    /**
     * 获取当前学期的重要时间节点列表。
     *
     * @param semester 学期,如 "2026-FALL", "2026-SPRING"
     * @return 按时间排序的节点列表,每个节点包含剩余天数和状态
     */
    public List<RegistrationDeadline> getUpcomingDeadlines(String semester) {
        LocalDate semesterStart = parseSemesterStart(semester);
        LocalDate semesterEnd = parseSemesterEnd(semester);
        LocalDateTime now = LocalDateTime.now();

        // 注册开始: 学期首日前三周的周一 9:00
        LocalDateTime registrationStart = LocalDateTime.of(
                semesterStart.minusWeeks(3).with(DayOfWeek.MONDAY),
                LocalTime.of(9, 0));

        // 增减课截止: 注册开始后 2 周
        LocalDateTime addDropDeadline = registrationStart.plusWeeks(2);

        // 注册关闭: 注册开始后 4 周
        LocalDateTime registrationClose = registrationStart.plusWeeks(4);

        // 成绩提交截止: 学期结束前 1 周 周五 17:00
        LocalDateTime gradeSubmissionDeadline = LocalDateTime.of(
                semesterEnd.minusWeeks(1).with(DayOfWeek.FRIDAY),
                LocalTime.of(17, 0));

        List<RegistrationDeadline> deadlines = new ArrayList<>();
        deadlines.add(buildDeadline("注册开始", registrationStart,
                "学期选课系统开放注册", now));
        deadlines.add(buildDeadline("增减课截止", addDropDeadline,
                "增减课程的最后期限", now));
        deadlines.add(buildDeadline("注册关闭", registrationClose,
                "选课系统关闭,无法再增减课程", now));
        deadlines.add(buildDeadline("成绩提交截止", gradeSubmissionDeadline,
                "教授提交成绩的最后期限", now));

        return deadlines;
    }

    /**
     * 获取最近一个即将到来的节点(用于 Header 提醒小部件)。
     *
     * @param semester 学期
     * @return 最近的未来节点,如无则返回 null
     */
    public RegistrationDeadline getNearestUpcoming(String semester) {
        List<RegistrationDeadline> deadlines = getUpcomingDeadlines(semester);
        LocalDateTime now = LocalDateTime.now();

        for (RegistrationDeadline d : deadlines) {
            if (d.getDate().isAfter(now)) {
                return d;
            }
        }
        return null;
    }

    /** 构建时间节点 DTO */
    private RegistrationDeadline buildDeadline(String name, LocalDateTime date,
                                               String description, LocalDateTime now) {
        long daysBetween = ChronoUnit.DAYS.between(now.toLocalDate(), date.toLocalDate());

        DeadlineStatus status;
        int daysRemaining;
        if (daysBetween < 0) {
            status = DeadlineStatus.PASSED;
            daysRemaining = 0;
        } else if (daysBetween == 0) {
            status = DeadlineStatus.ACTIVE;
            daysRemaining = 0;
        } else {
            status = DeadlineStatus.UPCOMING;
            daysRemaining = (int) daysBetween;
        }

        return new RegistrationDeadline(name, date, status, daysRemaining, description);
    }

    /**
     * 解析学期开始日期。
     * <p>
     * 规则: FALL → 该年 9 月初第一个周一, SPRING → 该年 1 月中旬第一个周一。
     */
    private LocalDate parseSemesterStart(String semester) {
        String[] parts = semester.split("-");
        int year = Integer.parseInt(parts[0]);
        String term = parts.length > 1 ? parts[1] : "FALL";

        if ("SPRING".equalsIgnoreCase(term)) {
            // 1 月中旬第一个周一
            return LocalDate.of(year, 1, 15)
                    .with(DayOfWeek.MONDAY);
        } else {
            // 9 月初第一个周一
            return LocalDate.of(year, 9, 1)
                    .with(DayOfWeek.MONDAY);
        }
    }

    /**
     * 解析学期结束日期。
     * <p>
     * 规则: FALL → 该年 12 月中旬, SPRING → 该年 5 月中旬。
     */
    private LocalDate parseSemesterEnd(String semester) {
        String[] parts = semester.split("-");
        int year = Integer.parseInt(parts[0]);
        String term = parts.length > 1 ? parts[1] : "FALL";

        if ("SPRING".equalsIgnoreCase(term)) {
            return LocalDate.of(year, 5, 15);
        } else {
            return LocalDate.of(year, 12, 15);
        }
    }
}
