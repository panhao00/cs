package com.crs.web.dto;

import com.crs.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 自助注册请求 DTO。
 * <p>
 * 学生/教授可自行注册账号,默认角色为 STUDENT。
 * 若需注册为 PROFESSOR,须提供 department 字段。
 */
@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度 3-50 字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度 6-100 字符")
    private String password;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 100, message = "姓名不超过 100 字符")
    private String name;

    @NotNull(message = "角色不能为空")
    private Role role;

    /** 仅教授需要 */
    private String department;
}
