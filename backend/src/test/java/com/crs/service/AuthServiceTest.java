package com.crs.service;

import com.crs.common.ApiException;
import com.crs.domain.Role;
import com.crs.domain.User;
import com.crs.repository.UserRepository;
import com.crs.security.CrsUserDetails;
import com.crs.security.JwtTokenProvider;
import com.crs.web.dto.LoginRequest;
import com.crs.web.dto.LoginResponse;
import com.crs.web.dto.RegisterRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;

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

    @Mock
    private PasswordEncoder passwordEncoder;

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

        // 默认: save 直接返回传入的 User (账户锁定持久化需要)
        lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
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
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(sampleUser));
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
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(sampleUser));
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
        User attacker = new User();
        attacker.setId(2L);
        attacker.setUsername("attacker");
        attacker.setPasswordHash("$2a$hashed");
        attacker.setRole(Role.STUDENT);
        attacker.setName("Attacker");
        attacker.setStatus(User.UserStatus.ACTIVE);

        when(userRepository.findByUsername("attacker")).thenReturn(Optional.of(attacker));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
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

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(sampleUser));
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
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(sampleUser));
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
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(sampleUser));
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
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(sampleUser));
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
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(sampleUser));
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("密码错误"));

        // Act
        assertThrows(BadCredentialsException.class, () ->
                authService.login(validRequest));

        // Assert
        verify(auditService).recordLoginFailed(eq("student"), contains("密码错误"));
        verify(auditService, never()).recordLoginSuccess(anyLong(), anyString());
    }

    // ==================== register ====================

    @Test
    @DisplayName("注册成功 - 学生注册返回 JWT")
    void register_student_returnsLoginResponse() {
        // Arrange
        RegisterRequest req = new RegisterRequest();
        req.setUsername("newstudent");
        req.setPassword("pass123");
        req.setName("New Student");
        req.setRole(Role.STUDENT);

        when(userRepository.findByUsername("newstudent")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass123")).thenReturn("$2a$hashed");
        when(tokenProvider.generate(anyLong(), eq("newstudent"), eq(Role.STUDENT))).thenReturn(MOCK_TOKEN);
        when(tokenProvider.getExpirationMs()).thenReturn(EXPIRATION_MS);

        // Capture the saved User
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(10L);
            return u;
        });

        // Act
        LoginResponse result = authService.register(req);

        // Assert
        assertNotNull(result);
        assertEquals(MOCK_TOKEN, result.getToken());
        assertEquals("newstudent", result.getUsername());
        assertEquals(Role.STUDENT, result.getRole());
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("pass123");
    }

    @Test
    @DisplayName("注册成功 - 教授注册需要院系")
    void register_professor_withDepartment_success() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("newprof");
        req.setPassword("pass123");
        req.setName("New Prof");
        req.setRole(Role.PROFESSOR);
        req.setDepartment("Math");

        when(userRepository.findByUsername("newprof")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass123")).thenReturn("$2a$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(11L);
            return u;
        });
        when(tokenProvider.generate(anyLong(), eq("newprof"), eq(Role.PROFESSOR))).thenReturn(MOCK_TOKEN);
        when(tokenProvider.getExpirationMs()).thenReturn(EXPIRATION_MS);

        LoginResponse result = authService.register(req);
        assertNotNull(result);
        assertEquals(Role.PROFESSOR, result.getRole());
    }

    @Test
    @DisplayName("注册失败 - 用户名已存在")
    void register_duplicateUsername_throwsConflict() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("student");
        req.setPassword("pass123");
        req.setName("Dup");
        req.setRole(Role.STUDENT);

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(sampleUser));

        assertThrows(ApiException.class, () -> authService.register(req));
    }

    @Test
    @DisplayName("注册失败 - 教授未提供院系")
    void register_professor_noDepartment_throwsBadRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("newprof");
        req.setPassword("pass123");
        req.setName("New Prof");
        req.setRole(Role.PROFESSOR);
        req.setDepartment(null);

        when(userRepository.findByUsername("newprof")).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> authService.register(req));
    }

    @Test
    @DisplayName("注册失败 - 不允许注册 REGISTRAR 角色")
    void register_registrarRole_throwsBadRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("newadmin");
        req.setPassword("pass123");
        req.setName("Admin");
        req.setRole(Role.REGISTRAR);

        when(userRepository.findByUsername("newadmin")).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> authService.register(req));
    }
}
