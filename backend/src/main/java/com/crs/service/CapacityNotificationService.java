package com.crs.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CapacityNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyOfferingFull(Long studentId, String courseId, Long offeringId, int enrolledCount, int maxCapacity) {
        Map<String, Object> payload = Map.of(
            "type", "OFFERING_FULL",
            "courseId", courseId,
            "offeringId", offeringId,
            "enrolledCount", enrolledCount,
            "maxCapacity", maxCapacity,
            "message", courseId + " 课程已满员，当前 " + enrolledCount + "/" + maxCapacity + " 人"
        );
        messagingTemplate.convertAndSend("/topic/user/" + studentId + "/capacity", payload);
        log.debug("Sent capacity warning to student {}: {}", studentId, courseId);
    }

    public void notifyCapacityChange(Long studentId, String courseId, Long offeringId, int enrolledCount, int maxCapacity) {
        Map<String, Object> payload = Map.of(
            "type", "CAPACITY_CHANGE",
            "courseId", courseId,
            "offeringId", offeringId,
            "enrolledCount", enrolledCount,
            "maxCapacity", maxCapacity
        );
        messagingTemplate.convertAndSend("/topic/user/" + studentId + "/capacity", payload);
    }
}
