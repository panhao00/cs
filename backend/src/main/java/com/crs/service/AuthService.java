package com.crs.service;

import com.crs.domain.Role;
import com.crs.domain.User;
import com.crs.repository.UserRepository;
import com.crs.security.JwtTokenProvider;
import com.crs.web.dto.LoginRequest;
import com.crs.web.dto.LoginResponse;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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

    /** 登录失败追踪: username -> 连续失败次数 */
    private final ConcurrentHashMap<String, AtomicInteger> loginAttempts = new ConcurrentHashMap<>();

    /** 连续失败上限 */
    private static final int MAX_ATTEMPTS = 5;

    /** 锁定时长(毫秒): 15分钟 */
    private static final long LOCK_DURATION_MS = 15 * 60 * 1000;

    /** 账户锁定时间: username -> 锁定截止时间戳 */
    private final ConcurrentHashMap<String, Long> lockedUntil = new ConcurrentHashMap<>();

    /**
     * 登录。
     *
     * @param request 用户名 + 密码
     * @return JWT 令牌 + 用户信息
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername();

        // 检查账户是否被锁定
        Long lockExpiry = lockedUntil.get(username);
        if (lockExpiry != null && System.currentTimeMillis() < lockExpiry) {
            long remainingMinutes = (lockExpiry - System.currentTimeMillis()) / 60_000 + 1;
            log.warn("账户 {} 已被锁定,剩余 {} 分钟", username, remainingMinutes);
            throw new LockedException("账户已锁定,请 " + remainingMinutes + " 分钟后重试");
        }
        // 锁定已过期,清除
        if (lockExpiry != null && System.currentTimeMillis() >= lockExpiry) {
            lockedUntil.remove(username);
            loginAttempts.remove(username);
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            username,
                            request.getPassword()));
        } catch (Exception e) {
            // 记录失败次数
            AtomicInteger attempts = loginAttempts.computeIfAbsent(username, k -> new AtomicInteger(0));
            int currentAttempts = attempts.incrementAndGet();

            auditService.recordLoginFailed(username, e.getMessage());

            if (currentAttempts >= MAX_ATTEMPTS) {
                lockedUntil.put(username, System.currentTimeMillis() + LOCK_DURATION_MS);
                log.warn("账户 {} 连续失败 {} 次,已锁定 15 分钟", username, currentAttempts);
                throw new LockedException("连续失败次数过多,账户已锁定 15 分钟");
            }

            log.warn("登录失败: username={}, 失败次数={}/{}", username, currentAttempts, MAX_ATTEMPTS);
            throw new BadCredentialsException("用户名或密码错误(剩余 " + (MAX_ATTEMPTS - currentAttempts) + " 次机会)");
        }

        // 登录成功: 清除失败计数
        loginAttempts.remove(username);
        lockedUntil.remove(username);

        var principal = (com.crs.security.CrsUserDetails) authentication.getPrincipal();
        Long userId = principal.getId();
        Role role = principal.getRole();

        User user = userRepository.findById(userId).orElseThrow();
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
}
