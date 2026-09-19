package com.crs.web;

import com.crs.security.SecurityContextHelper;
import com.crs.service.AuthService;
import com.crs.web.dto.ChangePasswordRequest;
import com.crs.web.dto.LoginRequest;
import com.crs.web.dto.LoginResponse;
import com.crs.web.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器: POST /auth/login, POST /auth/register
 * 对应用例 7.1 Login + 自助注册。
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SecurityContextHelper securityContextHelper;

    /**
     * 登录。
     * <p>
     * 基本流: 验证成功 → 200 + JWT。
     * 备选流: 验证失败 → 401。
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * 自助注册。
     * <p>
     * 学生/教授可自行注册账号,注册成功后自动返回 JWT(自动登录)。
     * 备选流: 用户名重复 → 409; 校验失败 → 400。
     */
    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    /**
     * 修改密码。
     * <p>
     * 已登录用户修改密码,需验证旧密码。
     */
    @PutMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = securityContextHelper.getCurrentUserId();
        authService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }
}
