package com.crs.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 登录请求 DTO。
 * 对应用例 7.1 Login 基本流: 用户输入用户名密码 → 系统验证。
 */
@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
