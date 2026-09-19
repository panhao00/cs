package com.crs.web;

import com.crs.service.SystemSettingService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统设置控制器: 注册主任配置系统参数。
 * <p>
 * 端点:
 * - GET    /settings       获取所有设置
 * - GET    /settings/{key} 获取单个设置
 * - PUT    /settings       批量更新设置
 * - PUT    /settings/{key} 更新单个设置
 * <p>
 * 安全: 强制 REGISTRAR 角色。
 */
@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SystemSettingController {

    private final SystemSettingService systemSettingService;
    private final RoleGuard roleGuard;

    /**
     * 获取所有系统设置。
     */
    @GetMapping
    public ResponseEntity<Map<String, String>> getAllSettings() {
        roleGuard.requireRegistrar();
        return ResponseEntity.ok(systemSettingService.getAllSettings());
    }

    /**
     * 获取单个系统设置。
     *
     * @param key 设置键
     */
    @GetMapping("/{key}")
    public ResponseEntity<Map<String, String>> getSetting(@PathVariable String key) {
        roleGuard.requireRegistrar();
        String value = systemSettingService.getSetting(key)
                .orElse(null);
        return ResponseEntity.ok(Map.of("key", key, "value", value != null ? value : ""));
    }

    /**
     * 批量更新系统设置。
     * <p>
     * 请求体: {"settings": {"key1": "value1", "key2": "value2"}}
     */
    @PutMapping
    public ResponseEntity<Map<String, String>> bulkUpdateSettings(
            @RequestBody Map<String, Object> body) {
        roleGuard.requireRegistrar();
        @SuppressWarnings("unchecked")
        Map<String, String> settings = (Map<String, String>) body.get("settings");
        systemSettingService.setSettings(settings);
        return ResponseEntity.ok(Map.of("message", "设置已更新"));
    }

    /**
     * 更新单个系统设置。
     *
     * @param key 设置键
     */
    @PutMapping("/{key}")
    public ResponseEntity<Map<String, String>> updateSetting(
            @PathVariable String key,
            @RequestBody Map<String, String> body) {
        roleGuard.requireRegistrar();
        String value = body.get("value");
        String description = body.get("description");
        systemSettingService.setSetting(key, value, description);
        return ResponseEntity.ok(Map.of("key", key, "value", value));
    }
}
