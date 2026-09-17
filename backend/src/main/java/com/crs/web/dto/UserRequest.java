package com.crs.web.dto;

import com.crs.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/**
 * 创建/更新用户请求 DTO。
 * 对应用例 7.6 / 7.7 Maintain Student/Professor。
 * <p>
 * password 字段: 创建时必填,更新时留空表示不修改密码(在 Service 层处理,
 * 避免使用 @NotBlank 强制校验导致更新场景失败)。
 */
@Getter
@Setter
public class UserRequest {

    @NotBlank(message = "username 不能为空")
    private String username;

    /** 密码: 创建时必填(在 Service 层校验),更新时留空表示不修改 */
    private String password;

    @NotNull(message = "role 不能为空")
    private Role role;

    @NotBlank(message = "name 不能为空")
    private String name;

    private String department;
    private LocalDate dob;
    private LocalDate graduationDate;
}
