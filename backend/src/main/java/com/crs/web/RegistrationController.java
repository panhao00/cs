package com.crs.web;

import com.crs.common.RegistrationPhase;
import com.crs.service.RegistrationService;
import com.crs.web.dto.CloseRegistrationResult;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 注册周期管理控制器: 实现用例 7.8 Close Registration。
 * <p>
 * 端点:
 * - GET   /registration/status              查询当前注册阶段
 * - GET   /registration/preview              预览关闭注册后的状态
 * - POST  /registration/close                关闭注册(含通知 Billing + 满员/未满通知)
 * - POST  /registration/phase/advance        推进到下一阶段(open→add-drop→closed)
 * <p>
 * 安全: 强制 REGISTRAR 角色。
 */
@RestController
@RequestMapping("/registration")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;
    private final RegistrationPhase registrationPhase;
    private final RoleGuard roleGuard;

    private static final String CURRENT_SEMESTER = "2026-FALL";

    /**
     * 查询当前注册阶段(任何已登录用户可查)。
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getStatus() {
        roleGuard.requireAnyLogin();
        return ResponseEntity.ok(Map.of(
                "phase", registrationService.getCurrentPhase(),
                "nextPhase", registrationPhase.getNextPhase(),
                "canAdvance", String.valueOf(registrationPhase.canAdvance()),
                "semester", CURRENT_SEMESTER
        ));
    }

    /**
     * 预览关闭注册后的状态(基本流 3 的预览)。
     * 只读,不修改任何数据。
     */
    @GetMapping("/preview")
    public ResponseEntity<CloseRegistrationResult> preview() {
        roleGuard.requireRegistrar();
        return ResponseEntity.ok(registrationService.previewCloseRegistration(CURRENT_SEMESTER));
    }

    /**
     * 关闭注册(基本流)。
     * <p>
     * 备选流:
     * - Registration Already Closed: 返回 409
     * - Billing System Down: 返回结果中 billingNotificationSent=false
     */
    @PostMapping("/close")
    public ResponseEntity<CloseRegistrationResult> closeRegistration(
            @RequestParam(required = false) String semester) {
        roleGuard.requireRegistrar();
        String sem = semester != null ? semester : CURRENT_SEMESTER;
        return ResponseEntity.ok(registrationService.closeRegistration(sem));
    }

    /**
     * 推进注册阶段到下一状态。
     * <p>
     * 转换规则: open → add-drop → closed
     * closed 之后不能再推进(返回 409)。
     */
    @PostMapping("/phase/advance")
    public ResponseEntity<Map<String, String>> advancePhase() {
        roleGuard.requireRegistrar();
        String newPhase = registrationPhase.advance();
        return ResponseEntity.ok(Map.of(
                "phase", newPhase,
                "message", "注册阶段已推进至: " + newPhase
        ));
    }
}
