package com.crs.web;

import com.crs.service.WaitlistService;
import com.crs.web.dto.WaitlistDTO;
import com.crs.common.ApiException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 候补名单控制器。
 * <p>
 * 端点:
 * - POST   /waitlist/join               加入候补(学生)
 * - DELETE /waitlist/cancel/{offeringId} 取消候补(学生)
 * - GET    /waitlist/me                 查看我的候补列表(学生)
 * - GET    /waitlist/offering/{offeringId} 查看某课程候补队列(教授/Registrar)
 */
@Slf4j
@RestController
@RequestMapping("/waitlist")
@RequiredArgsConstructor
public class WaitlistController {

    private final WaitlistService waitlistService;
    private final RoleGuard roleGuard;

    /**
     * 加入候补名单(学生)。
     */
    @PostMapping("/join")
    public ResponseEntity<WaitlistDTO> joinWaitlist(@RequestBody Map<String, Long> body) {
        Long userId = roleGuard.requireStudent();
        Long offeringId = body.get("offeringId");
        if (offeringId == null) {
            throw com.crs.common.ApiException.badRequest("offeringId 不能为空");
        }
        WaitlistDTO dto = waitlistService.joinWaitlist(userId, offeringId);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * 取消候补(学生)。
     */
    @DeleteMapping("/cancel/{offeringId}")
    public ResponseEntity<Map<String, String>> cancelWaitlist(@PathVariable Long offeringId) {
        Long userId = roleGuard.requireStudent();
        waitlistService.cancelWaitlist(userId, offeringId);
        return ResponseEntity.ok(Map.of("message", "已取消候补"));
    }

    /**
     * 查看我的候补列表(学生)。
     */
    @GetMapping("/me")
    public ResponseEntity<List<WaitlistDTO>> getMyWaitlists() {
        Long userId = roleGuard.requireStudent();
        return ResponseEntity.ok(waitlistService.getMyWaitlists(userId));
    }

    /**
     * 查看某课程的候补队列(教授/Registrar)。
     */
    @GetMapping("/offering/{offeringId}")
    public ResponseEntity<List<WaitlistDTO>> getOfferingWaitlist(@PathVariable Long offeringId) {
        // 仅该课程的授课教授或注册主任可查看
        Long userId = roleGuard.requireAnyLogin();
        boolean isInstructor = waitlistService.isInstructorOfOffering(userId, offeringId);
        if (!isInstructor && !roleGuard.hasRole("REGISTRAR")) {
            throw ApiException.forbidden("只有该课程的授课教授或注册主任可查看候补队列");
        }
        return ResponseEntity.ok(waitlistService.getOfferingWaitlist(offeringId));
    }
}
