package com.crs.web;

import com.crs.common.ApiException;
import com.crs.security.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 控制器层角色校验辅助:统一处理 STUDENT/PROFESSOR/REGISTRAR 角色强制 + 水平越权防护。
 * <p>
 * 用法:在控制器方法首行调用 requireXxx() 获取当前用户 ID。
 */
@Component
@RequiredArgsConstructor
public class RoleGuard {

    private final SecurityContextHelper securityContextHelper;

    /** 强制 STUDENT 角色,返回当前用户 ID */
    public Long requireStudent() {
        return requireRole("STUDENT", "只有学生可以操作课表");
    }

    /** 强制 PROFESSOR 角色,返回当前教授 ID */
    public Long requireProfessor() {
        return requireRole("PROFESSOR", "只有教授可以操作授课/成绩");
    }

    /** 强制 REGISTRAR 角色,返回当前注册主任 ID */
    public Long requireRegistrar() {
        return requireRole("REGISTRAR", "只有注册主任可以执行此操作");
    }

    /** 任何已登录用户,返回用户 ID */
    public Long requireAnyLogin() {
        Long userId = securityContextHelper.getCurrentUserId();
        if (userId == null) {
            throw ApiException.unauthorized("未登录");
        }
        return userId;
    }

    private Long requireRole(String role, String errorMessage) {
        Long userId = securityContextHelper.getCurrentUserId();
        if (userId == null) {
            throw ApiException.unauthorized("未登录");
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean hasRole = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
        if (!hasRole) {
            throw ApiException.forbidden(errorMessage);
        }
        return userId;
    }
}
