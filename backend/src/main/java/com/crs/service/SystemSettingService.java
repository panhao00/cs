package com.crs.service;

import com.crs.common.ApiException;
import com.crs.domain.SystemSetting;
import com.crs.repository.SystemSettingRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统设置服务: 提供系统参数的读写操作。
 * <p>
 * 用于注册主任配置学期、注册阶段、容量限制等运行时参数。
 * 所有设置以键值对形式持久化到 system_setting 表。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;

    /**
     * 获取单个设置值。
     *
     * @param key 设置键
     * @return 设置值的 Optional
     */
    @Transactional(readOnly = true)
    public Optional<String> getSetting(String key) {
        return systemSettingRepository.findBySettingKey(key)
                .map(SystemSetting::getSettingValue);
    }

    /**
     * 批量获取多个设置值。
     *
     * @param keys 设置键列表
     * @return 键值对映射(仅包含存在的设置)
     */
    @Transactional(readOnly = true)
    public Map<String, String> getSettings(List<String> keys) {
        Map<String, String> result = new LinkedHashMap<>();
        systemSettingRepository.findBySettingKeyIn(keys)
                .forEach(s -> result.put(s.getSettingKey(), s.getSettingValue()));
        return result;
    }

    /**
     * 获取所有设置。
     *
     * @return 所有设置的键值对映射
     */
    @Transactional(readOnly = true)
    public Map<String, String> getAllSettings() {
        Map<String, String> result = new LinkedHashMap<>();
        systemSettingRepository.findAll()
                .forEach(s -> result.put(s.getSettingKey(), s.getSettingValue()));
        return result;
    }

    /**
     * 设置单个配置项(存在则更新,不存在则创建)。
     *
     * @param key         设置键
     * @param value       设置值
     * @param description 设置描述(可为 null)
     */
    @Transactional
    public void setSetting(String key, String value, String description) {
        if (key == null || key.isBlank()) {
            throw ApiException.badRequest("设置键不能为空");
        }
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest("设置值不能为空");
        }

        SystemSetting setting = systemSettingRepository.findBySettingKey(key)
                .orElseGet(() -> {
                    SystemSetting s = new SystemSetting();
                    s.setSettingKey(key);
                    return s;
                });
        setting.setSettingValue(value);
        if (description != null) {
            setting.setDescription(description);
        }
        systemSettingRepository.save(setting);
        log.info("系统设置已更新: {} = {}", key, value);
    }

    /**
     * 批量设置配置项。
     *
     * @param settings 键值对映射
     */
    @Transactional
    public void setSettings(Map<String, String> settings) {
        if (settings == null || settings.isEmpty()) {
            throw ApiException.badRequest("设置内容不能为空");
        }
        settings.forEach((key, value) -> setSetting(key, value, null));
    }
}
