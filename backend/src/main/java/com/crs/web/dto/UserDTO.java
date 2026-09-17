package com.crs.web.dto;

import com.crs.domain.Role;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户视图 DTO(注册主任维护学生/教授用例用)。
 * 敏感字段(密码哈希、SSN)只在服务端处理,不通过此 DTO 暴露。
 */
@Getter
@Setter
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private Role role;
    private String name;
    private String department;
    private LocalDate dob;
    private LocalDate graduationDate;
    private String status;  // ACTIVE / INACTIVE
}
