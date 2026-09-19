package com.crs.common;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 学期配置: 从 application.yml 读取当前学期,避免硬编码。
 */
@Component
@Getter
public class SemesterConfig {

    private final String currentSemester;
    private final String previousSemester;

    public SemesterConfig(
            @Value("${crs.registration.current-semester:2026-FALL}") String currentSemester) {
        this.currentSemester = currentSemester;
        // 计算上学期: FALL → SPRING, SPRING → FALL-1
        this.previousSemester = calculatePreviousSemester(currentSemester);
    }

    private String calculatePreviousSemester(String semester) {
        String[] parts = semester.split("-");
        if (parts.length != 2) {
            return "2026-SPRING"; // 默认值
        }
        String year = parts[0];
        String term = parts[1];

        if ("FALL".equals(term)) {
            return year + "-SPRING";
        } else {
            // SPRING → 上一年的 FALL
            int prevYear = Integer.parseInt(year) - 1;
            return prevYear + "-FALL";
        }
    }
}
