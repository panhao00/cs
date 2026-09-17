package com.crs.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 从 SecurityContext 获取当前登录用户 ID 的工具。
 * 用于控制器层获取当前操作者(水平越权防护)。
 */
@Component
public class SecurityContextHelper {

    /** 获取当前用户 ID;未登录返回 null */
    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            return null;
        }
        return (Long) auth.getPrincipal();
    }
}
