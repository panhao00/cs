package com.crs.web;

import com.crs.common.ApiException;
import com.crs.common.GlobalExceptionHandler;
import com.crs.domain.Role;
import com.crs.security.SecurityContextHelper;
import com.crs.service.AuthService;
import com.crs.web.dto.LoginRequest;
import com.crs.web.dto.LoginResponse;
import com.crs.web.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController 控制器层测试。
 * <p>
 * 使用 MockMvc standalone 模式,Mock AuthService 层,
 * 包含 GlobalExceptionHandler 以验证异常 → HTTP 映射。
 * <p>
 * 覆盖:
 * - POST /auth/login 成功/失败
 * - POST /auth/register 成功/重复
 * - GET /health 健康检查
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController 集成测试")
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @Mock
    private SecurityContextHelper securityContextHelper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new AuthController(authService, securityContextHelper),
                        new HealthController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ==================== POST /auth/login ====================

    @Test
    @DisplayName("登录成功 - 返回 200 和 JWT 令牌")
    void login_success() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("student");
        request.setPassword("student");

        LoginResponse response = new LoginResponse(
                "mock-jwt-token", "Bearer", 1L,
                "student", "Charlie Brown", Role.STUDENT,
                System.currentTimeMillis() + 86400000L);
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value("student"))
                .andExpect(jsonPath("$.name").value("Charlie Brown"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    @DisplayName("登录失败 - 密码错误返回 401")
    void login_wrongPassword() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("student");
        request.setPassword("wrongPassword");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("用户名或密码错误"));

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    @DisplayName("登录失败 - 用户不存在返回 401")
    void login_userNotFound() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("nonexistent");
        request.setPassword("password123");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("用户名或密码错误"));

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    // ==================== POST /auth/register ====================

    @Test
    @DisplayName("注册成功 - 返回 201 和 JWT 令牌")
    void register_success() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newstudent");
        request.setPassword("pass123");
        request.setName("New Student");
        request.setRole(Role.STUDENT);

        LoginResponse response = new LoginResponse(
                "mock-jwt-token", "Bearer", 10L,
                "newstudent", "New Student", Role.STUDENT,
                System.currentTimeMillis() + 86400000L);
        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.userId").value(10))
                .andExpect(jsonPath("$.username").value("newstudent"))
                .andExpect(jsonPath("$.name").value("New Student"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    @DisplayName("注册失败 - 用户名重复返回 409")
    void register_duplicateUsername() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("student");
        request.setPassword("pass123");
        request.setName("Dup Student");
        request.setRole(Role.STUDENT);

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(ApiException.conflict("用户名已存在: student"));

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("用户名已存在: student"));
    }

    // ==================== GET /health ====================

    @Test
    @DisplayName("健康检查 - 返回 200 和服务状态")
    void health() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("Course Registration System (CRS)"))
                .andExpect(jsonPath("$.version").value("0.1.0"));
    }
}
