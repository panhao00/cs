package com.crs.web;

import com.crs.security.SecurityContextHelper;
import com.crs.service.CatalogService;
import com.crs.web.dto.CourseOfferingDTO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 课程目录控制器(只读)。
 * 对应需求: 新系统只读访问 Course Catalog。
 */
@RestController
@RequestMapping("/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;
    private final SecurityContextHelper securityContextHelper;

    /**
     * 查询某学期可用课程开设列表。
     * 任何已登录用户均可查询(选课时学生/教授均需)。
     */
    @GetMapping("/offerings")
    public ResponseEntity<List<CourseOfferingDTO>> getOfferings(
            @RequestParam String semester) {
        // 任何已登录用户可访问(JwtAuthenticationFilter 已强制认证)
        securityContextHelper.getCurrentUserId(); // 触发鉴权检查
        return ResponseEntity.ok(catalogService.getAvailableOfferings(semester));
    }

    /**
     * 查询单个课程开设详情(不缓存,容量实时)。
     */
    @GetMapping("/offerings/{id}")
    public ResponseEntity<CourseOfferingDTO> getOffering(@PathVariable Long id) {
        return ResponseEntity.of(catalogService.getOffering(id));
    }
}
