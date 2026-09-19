package com.crs.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 管理员重置用户密码请求 DTO。
 */
@Getter
@Setter
public class ResetPasswordRequest {

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度 6-100 字符")
    private String newPassword;
}
