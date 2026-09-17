package com.crs.service;

import com.crs.domain.Role;
import com.crs.domain.User;
import com.crs.repository.UserRepository;
import com.crs.security.CrsUserDetails;
import com.crs.security.JwtTokenProvider;
import com.crs.web.dto.LoginRequest;
import com.crs.web.dto.LoginResponse;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AuthService 单元测试。
 * <p>
 * 覆盖用例 7.1 Login 的核心业务:
 * - 登录成功返回 JWT
 * - 登录失败抛出异常
 * - JWT token 内容校验
 * - 审计日志记录
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuthService authService;

    private static final String MOCK_TOKEN = "eyJhbGciOiJIUzI1NiJ9.mock.token";
    private static final long EXPIRATION_MS = 3600000L;

    private User sampleUser;
    private LoginRequest validRequest;

    @BeforeEach
    void setUp() {
        // 示例用户
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setUsername("student");
        sampleUser.setPasswordHash("$2a$hashed");
        sampleUser.setRole(Role.STUDENT);
        sampleUser.setName("Charlie Brown");
        sampleUser.setStatus(User.UserStatus.ACTIVE);

        // 有效登录请求
        validRequest = new LoginRequest();
        validRequest.setUsername("student");
        validRequest.setPassword("student");
    }

    // ==================== 辅助方法 ====================

    /** 创建带 CrsUserDetails 主体的 Authentication mock */
    private Authentication createSuccessfulAuth(Long userId, Role role) {
        CrsUserDetails userDetails = mock(CrsUserDetails.class);
        when(userDetails.getId()).thenReturn(userId);
        when(userDetails.getRole()).thenReturn(role);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        return auth;
    }

    // ==================== login ====================

    @Test
    @DisplayName("登录成功 - 返回有效的 LoginResponse")
    void login_validCredentials_returnsLoginResponse() {
        // Arrange
        Authentication auth = createSuccessfulAuth(1L, Role.STUDENT);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(tokenProvider.generate(1L, "student", Role.STUDENT)).thenReturn(MOCK_TOKEN);
        when(tokenProvider.getExpirationMs()).thenReturn(EXPIRATION_MS);

        // Act
        LoginResponse result = authService.login(validRequest);

        // Assert
        assertNotNull(result);
        assertEquals(MOCK_TOKEN, result.getToken());
        assertEquals("Bearer", result.getTokenType());
        assertEquals(1L, result.getUserId());
        assertEquals("student", result.getUsername());
        assertEquals("Charlie Brown", result.getName());
        assertEquals(Role.STUDENT, result.getRole());
        assertNotNull(result.getExpiresAt());
    }

    @Test
    @DisplayName("登录失败 - 密码错误抛出 BadCredentialsException")
    void login_invalidPassword_throwsBadCredentials() {
        // Arrange
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("用户名或密码错误"));

        // Act & Assert
        assertThrows(BadCredentialsException.class, () ->
                authService.login(validRequest));
        verify(auditService).recordLoginFailed(eq("student"), contains("用户名或密码错误"));
        verify(auditService, never()).recordLoginSuccess(anyLong(), anyString());
    }

    @Test
    @DisplayName("登录失败 - 连续 5 次失败后账户锁定")
    void login_5ConsecutiveFailures_locksAccount() {
        // Arrange
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("用户名或密码错误"));

        LoginRequest req = new LoginRequest();
        req.setUsername("attacker");
        req.setPassword("wrong");

        // Act: 连续调用 5 次
        for (int i = 0; i < 4; i++) {
            assertThrows(BadCredentialsException.class, () ->
                    authService.login(req));
        }
        // 第 5 次应抛 LockedException
        assertThrows(org.springframework.security.authentication.LockedException.class, () ->
                authService.login(req));
    }

    @Test
    @DisplayName("登录失败 - 未录入密码时抛出异常")
    void login_nullPassword_throwsBadCredentials() {
        // Arrange
        LoginRequest nullPassReq = new LoginRequest();
        nullPassReq.setUsername("student");
        nullPassReq.setPassword(null);

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("用户名或密码错误"));

        // Act & Assert
        assertThrows(BadCredentialsException.class, () ->
                authService.login(nullPassReq));
    }

    // ==================== JWT 内容验证 ====================

    @Test
    @DisplayName("登录成功 - JWT token 包含正确的用户信息")
    void login_success_tokenContainsCorrectUserInfo() {
        // Arrange
        Authentication auth = createSuccessfulAuth(1L, Role.STUDENT);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(tokenProvider.generate(1L, "student", Role.STUDENT)).thenReturn(MOCK_TOKEN);
        when(tokenProvider.getExpirationMs()).thenReturn(EXPIRATION_MS);

        // Act
        LoginResponse result = authService.login(validRequest);

        // Assert: 验证 tokenProvider 被正确调用，参数包含 userId, username, role
        verify(tokenProvider).generate(1L, "student", Role.STUDENT);
        assertEquals(MOCK_TOKEN, result.getToken());
        assertEquals(Role.STUDENT, result.getRole());
    }

    @Test
    @DisplayName("登录成功 - JWT 过期时间基于 tokenProvider 配置")
    void login_success_expirationSetCorrectly() {
        // Arrange
        long customExpiration = 7200000L; // 2 小时
        Authentication auth = createSuccessfulAuth(1L, Role.STUDENT);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(tokenProvider.generate(1L, "student", Role.STUDENT)).thenReturn(MOCK_TOKEN);
        when(tokenProvider.getExpirationMs()).thenReturn(customExpiration);

        // Act
        LoginResponse result = authService.login(validRequest);

        // Assert: expiresAt 应接近 currentTimeMillis + expirationMs
        long expectedMin = System.currentTimeMillis() + customExpiration - 1000;
        long expectedMax = System.currentTimeMillis() + customExpiration + 1000;
        assertTrue(result.getExpiresAt() >= expectedMin && result.getExpiresAt() <= expectedMax);
    }

    // ==================== 审计日志 ====================

    @Test
    @DisplayName("登录成功 - 记录审计日志")
    void login_success_recordsAuditLog() {
        // Arrange
        Authentication auth = createSuccessfulAuth(1L, Role.STUDENT);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(tokenProvider.generate(1L, "student", Role.STUDENT)).thenReturn(MOCK_TOKEN);
        when(tokenProvider.getExpirationMs()).thenReturn(EXPIRATION_MS);

        // Act
        authService.login(validRequest);

        // Assert
        verify(auditService).recordLoginSuccess(1L, "student");
        verify(auditService, never()).recordLoginFailed(anyString(), anyString());
    }

    @Test
    @DisplayName("登录失败 - 记录失败审计日志")
    void login_failure_recordsFailedAuditLog() {
        // Arrange
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("密码错误"));

        // Act
        assertThrows(BadCredentialsException.class, () ->
                authService.login(validRequest));

        // Assert
        verify(auditService).recordLoginFailed(eq("student"), contains("密码错误"));
        verify(auditService, never()).recordLoginSuccess(anyLong(), anyString());
    }
}
