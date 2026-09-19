package com.crs;

import com.crs.domain.SystemSetting;
import com.crs.repository.SystemSettingRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 系统设置初始化: 创建默认系统参数(若不存在)。
 * <p>
 * 默认设置:
 * - current_semester: "2026-FALL"
 * - previous_semester: "2026-SPRING"
 * - registration.phase: "open"
 * - max_primary_courses: "4"
 * - max_alternate_courses: "2"
 * - offering_max_students: "10"
 * - offering_min_students: "3"
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettingsInitializer implements CommandLineRunner {

    private final SystemSettingRepository systemSettingRepository;

    /** 默认系统设置 */
    private static final Map<String, String> DEFAULT_SETTINGS = Map.of(
            "current_semester", "2026-FALL",
            "previous_semester", "2026-SPRING",
            "registration.phase", "open",
            "max_primary_courses", "4",
            "max_alternate_courses", "2",
            "offering_max_students", "10",
            "offering_min_students", "3"
    );

    @Override
    public void run(String... args) {
        DEFAULT_SETTINGS.forEach((key, value) -> {
            if (systemSettingRepository.findBySettingKey(key).isEmpty()) {
                SystemSetting setting = new SystemSetting();
                setting.setSettingKey(key);
                setting.setSettingValue(value);
                systemSettingRepository.save(setting);
                log.info("初始化系统设置: {} = {}", key, value);
            }
        });
        log.info("系统设置初始化完成");
    }
}
