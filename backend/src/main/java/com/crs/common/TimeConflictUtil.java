package com.crs.common;

import java.util.HashSet;
import java.util.Set;

/**
 * 时间冲突检测工具类。
 * <p>
 * 提供课程时间冲突的核心判断逻辑,供 ScheduleService / TeachingService 复用。
 * <ul>
 *   <li>日期重叠: 两个日期字符串(如 "MWF" 与 "TR")共享任一字符即视为同天有课</li>
 *   <li>时间重叠: 将 "HH:mm-HH:mm" 解析为 LocalTime 后用 start1 &lt; end2 &amp;&amp; start2 &lt; end1 判断</li>
 *   <li>完整冲突: 同天且时间重叠</li>
 * </ul>
 */
public final class TimeConflictUtil {

    private TimeConflictUtil() {
        // utility class
    }

    /**
     * 解析时间范围字符串为起止分钟数。
     *
     * @param timeRange 格式 "HH:mm-HH:mm", 如 "09:00-10:30"
     * @return int[2]: {startMinutes, endMinutes}
     * @throws IllegalArgumentException 格式非法时抛出
     */
    public static int[] parseTimeRange(String timeRange) {
        if (timeRange == null || timeRange.isBlank()) {
            throw new IllegalArgumentException("时间范围不能为空");
        }
        String[] parts = timeRange.split("-");
        if (parts.length != 2) {
            throw new IllegalArgumentException(
                    "时间范围格式错误,期望 'HH:mm-HH:mm',实际: " + timeRange);
        }
        int start = parseTimeToMinutes(parts[0].trim());
        int end = parseTimeToMinutes(parts[1].trim());
        if (start >= end) {
            throw new IllegalArgumentException(
                    "起始时间必须早于结束时间: " + timeRange);
        }
        return new int[]{start, end};
    }

    /**
     * 解析 "HH:mm" 为当天分钟数。
     *
     * @param time 格式 "HH:mm", 如 "09:30"
     * @return 从 00:00 起的分钟数
     */
    public static int parseTimeToMinutes(String time) {
        if (time == null || time.isBlank()) {
            throw new IllegalArgumentException("时间不能为空");
        }
        String[] hm = time.split(":");
        if (hm.length != 2) {
            throw new IllegalArgumentException(
                    "时间格式错误,期望 'HH:mm',实际: " + time);
        }
        int hours = Integer.parseInt(hm[0].trim());
        int minutes = Integer.parseInt(hm[1].trim());
        if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59) {
            throw new IllegalArgumentException(
                    "时间值越界: " + time);
        }
        return hours * 60 + minutes;
    }

    /**
     * 解析日期字符串为单字符集合。
     * <p>
     * "MWF" → {'M','W','F'}, "TTh" → {'T','h'} (去重)。
     *
     * @param days 如 "MWF"、"TR"、"TTh"
     * @return 日期字符集合
     */
    public static Set<Character> parseDays(String days) {
        Set<Character> result = new HashSet<>();
        if (days != null) {
            for (char c : days.toCharArray()) {
                result.add(c);
            }
        }
        return result;
    }

    /**
     * 判断两个日期字符串是否有共同日期。
     *
     * @param days1 如 "MWF"
     * @param days2 如 "TR"
     * @return true 表示至少有一天重叠
     */
    public static boolean hasDayOverlap(String days1, String days2) {
        if (days1 == null || days2 == null) {
            return false;
        }
        Set<Character> d1 = parseDays(days1);
        for (char c : days2.toCharArray()) {
            if (d1.contains(c)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断两个时间范围是否有重叠(不考虑日期)。
     * <p>
     * 重叠条件: start1 &lt; end2 &amp;&amp; start2 &lt; end1。
     *
     * @param timeRange1 如 "09:00-10:30"
     * @param timeRange2 如 "10:00-11:30"
     * @return true 表示时间有重叠
     */
    public static boolean hasTimeOverlap(String timeRange1, String timeRange2) {
        if (timeRange1 == null || timeRange2 == null) {
            return false;
        }
        try {
            int[] r1 = parseTimeRange(timeRange1);
            int[] r2 = parseTimeRange(timeRange2);
            return r1[0] < r2[1] && r2[0] < r1[1];
        } catch (IllegalArgumentException e) {
            // 解析失败时回退到精确匹配
            return timeRange1.equals(timeRange2);
        }
    }

    /**
     * 综合判断两组课程时间是否冲突。
     * <p>
     * 冲突 = 有共同日期 AND 时间段重叠。
     *
     * @param days1  第一组日期, 如 "MWF"
     * @param times1 第一组时间, 如 "09:00-10:30"
     * @param days2  第二组日期, 如 "MW"
     * @param times2 第二组时间, 如 "10:00-11:30"
     * @return true 表示存在冲突
     */
    public static boolean isConflict(String days1, String times1,
                                     String days2, String times2) {
        if (days1 == null || times1 == null || days2 == null || times2 == null) {
            return false;
        }
        return hasDayOverlap(days1, days2) && hasTimeOverlap(times1, times2);
    }
}
