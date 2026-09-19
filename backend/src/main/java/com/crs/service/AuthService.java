package com.crs.service;

import com.crs.common.ApiException;
import com.crs.domain.Role;
import com.crs.domain.User;
import com.crs.repository.UserRepository;
import com.crs.security.JwtTokenProvider;
import com.crs.web.dto.LoginRequest;
import com.crs.web.dto.LoginResponse;
import com.crs.web.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证服务: 实现用例 7.1 Login。
 * <p>
 * 基本流: 用户输入用户名密码 → 系统验证 → 登录成功(返回 JWT)。
 * 备选流: 用户名/密码无效 → 返回 401,可重试。
 * <p>
 * 后置: 成功则返回 JWT 令牌;失败则系统状态不变。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;

    /** 连续失败上限 */
    private static final int MAX_ATTEMPTS = 5;

    /** 锁定时长(毫秒): 15分钟 */
    private static final long LOCK_DURATION_MS = 15 * 60 * 1000;

    /**
     * 登录。
     *
     * @param request 用户名 + 密码
     * @return JWT 令牌 + 用户信息
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername();

        // 查找用户并检查账户锁定状态
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("用户名或密码错误"));

        if (user.isLocked()) {
            long remainingMinutes = java.time.Duration.between(
                    java.time.LocalDateTime.now(), user.getLockedUntil()).toMinutes() + 1;
            log.warn("账户 {} 已被锁定,剩余 {} 分钟", username, remainingMinutes);
            throw new LockedException("账户已锁定,请 " + remainingMinutes + " 分钟后重试");
        }

        // 锁定已过期时清除锁定状态
        if (user.getLockedUntil() != null
                && java.time.LocalDateTime.now().isAfter(user.getLockedUntil())) {
            user.clearLoginAttempts();
            userRepository.save(user);
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            username,
                            request.getPassword()));
        } catch (Exception e) {
            // 记录失败次数
            user.recordFailedAttempt(MAX_ATTEMPTS, LOCK_DURATION_MS);
            userRepository.save(user);

            auditService.recordLoginFailed(username, e.getMessage());

            if (user.isLocked()) {
                log.warn("账户 {} 连续失败 {} 次,已锁定 15 分钟", username, user.getFailedLoginAttempts());
                throw new LockedException("连续失败次数过多,账户已锁定 15 分钟");
            }

            log.warn("登录失败: username={}, 失败次数={}/{}", username,
                    user.getFailedLoginAttempts(), MAX_ATTEMPTS);
            throw new BadCredentialsException("用户名或密码错误(剩余 "
                    + (MAX_ATTEMPTS - user.getFailedLoginAttempts()) + " 次机会)");
        }

        // 登录成功: 清除失败计数
        user.clearLoginAttempts();
        userRepository.save(user);

        var principal = (com.crs.security.CrsUserDetails) authentication.getPrincipal();
        Long userId = principal.getId();
        Role role = principal.getRole();

        String token = tokenProvider.generate(userId, user.getUsername(), role);
        long expiresAt = System.currentTimeMillis() + tokenProvider.getExpirationMs();

        auditService.recordLoginSuccess(userId, user.getUsername());

        return new LoginResponse(
                token,
                "Bearer",
                userId,
                user.getUsername(),
                user.getName(),
                role,
                expiresAt);
    }

    /**
     * 自助注册。
     * <p>
     * 学生/教授可自行注册账号。
     * 校验: 用户名唯一、密码长度 >= 6、角色合法性
     */
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        // 校验用户名唯一
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw ApiException.conflict("用户名已存在: " + request.getUsername());
        }

        // 校验角色: 只允许 STUDENT 和 PROFESSOR 自助注册
        if (request.getRole() != Role.STUDENT && request.getRole() != Role.PROFESSOR) {
            throw ApiException.badRequest("不支持注册该角色");
        }

        // 教授必须提供院系
        if (request.getRole() == Role.PROFESSOR
                && (request.getDepartment() == null || request.getDepartment().isBlank())) {
            throw ApiException.badRequest("教授角色必须填写所属院系");
        }

        // 密码强度校验
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw ApiException.badRequest("密码长度至少 6 位");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setName(request.getName());
        user.setDepartment(request.getDepartment());
        user.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(user);

        log.info("新用户注册: username={}, role={}", user.getUsername(), user.getRole());
        auditService.recordRegister(user.getId(), user.getUsername());

        // 注册成功后直接返回 JWT(自动登录)
        String token = tokenProvider.generate(user.getId(), user.getUsername(), user.getRole());
        long expiresAt = System.currentTimeMillis() + tokenProvider.getExpirationMs();

        return new LoginResponse(
                token,
                "Bearer",
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getRole(),
                expiresAt);
    }

    /**
     * 修改密码。
     * <p>
     * 已登录用户修改密码,需验证旧密码。
     *
     * @param userId      当前用户 ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("用户不存在"));

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw ApiException.badRequest("原密码错误");
        }

        // 密码强度校验
        if (newPassword == null || newPassword.length() < 6) {
            throw ApiException.badRequest("新密码长度至少 6 位");
        }

        // 编码新密码并保存
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("用户修改密码成功: userId={}, username={}", userId, user.getUsername());
    }
}
