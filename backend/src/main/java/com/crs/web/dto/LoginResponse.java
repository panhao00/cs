package com.crs.web.dto;

import com.crs.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 登录成功响应 DTO。
 */
@Getter
@Setter
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String tokenType;  // "Bearer"
    private Long userId;
    private String username;
    private String name;
    private Role role;

    /** 过期时间(毫秒时间戳) */
    private Long expiresAt;
}
