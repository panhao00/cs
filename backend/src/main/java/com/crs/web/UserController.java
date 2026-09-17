package com.crs.web;

import com.crs.domain.Role;
import com.crs.domain.User;
import com.crs.service.UserService;
import com.crs.web.dto.UserDTO;
import com.crs.web.dto.UserRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户控制器: 实现用例 7.6 Maintain Student / 7.7 Maintain Professor。
 * <p>
 * 端点:
 * - GET    /users?role=                  用户列表(可选按角色过滤)
 * - GET    /users/{id}                   用户详情
 * - POST   /users                        创建用户
 * - PUT    /users/{id}                   更新用户
 * - PATCH  /users/{id}/status?status=    设置状态(ACTIVE/INACTIVE)
 * - DELETE /users/{id}                   删除用户(必须先 INACTIVE)
 * <p>
 * 安全: 强制 REGISTRAR 角色;敏感字段不通过 DTO 暴露。
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('REGISTRAR')")
public class UserController {

    private final UserService userService;
    private final RoleGuard roleGuard;

    /**
     * 查询用户列表(可按角色过滤)。
     */
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers(
            @RequestParam(required = false) Role role) {
        roleGuard.requireRegistrar();
        return ResponseEntity.ok(userService.getAllUsers(role));
    }

    /**
     * 查询单个用户。
     * 备选流 Student Not Found: 返回 404。
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getById(@PathVariable Long id) {
        roleGuard.requireRegistrar();
        return ResponseEntity.ok(userService.getById(id));
    }

    /**
     * 创建用户(基本流 3-6)。
     * 备选流 Invalid Information: 校验失败 → 400。
     */
    @PostMapping
    public ResponseEntity<UserDTO> create(@Valid @RequestBody UserRequest request) {
        roleGuard.requireRegistrar();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.create(request));
    }

    /**
     * 更新用户(基本流 3-6)。
     * 密码字段留空表示不修改。
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> update(@PathVariable Long id,
                                          @Valid @RequestBody UserRequest request) {
        roleGuard.requireRegistrar();
        return ResponseEntity.ok(userService.update(id, request));
    }

    /**
     * 设置用户状态(ACTIVE/INACTIVE)。
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<UserDTO> setStatus(@PathVariable Long id,
                                              @RequestParam User.UserStatus status) {
        roleGuard.requireRegistrar();
        return ResponseEntity.ok(userService.setStatus(id, status));
    }

    /**
     * 删除用户(必须先设为 INACTIVE)。
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roleGuard.requireRegistrar();
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
