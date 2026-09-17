package com.crs.service;

import com.crs.common.ApiException;
import com.crs.domain.Role;
import com.crs.domain.User;
import com.crs.repository.UserRepository;
import com.crs.web.dto.UserDTO;
import com.crs.web.dto.UserRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserService 单元测试。
 * <p>
 * 覆盖用例 7.6 / 7.7 Maintain Student / Professor 的核心业务:
 * - 创建/更新/删除用户
 * - 用户名校验、角色校验
 * - 审计日志记录
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private UserService userService;

    private User sampleStudent;
    private UserRequest sampleStudentRequest;

    @BeforeEach
    void setUp() {
        // 创建示例学生实体
        sampleStudent = new User();
        sampleStudent.setId(1L);
        sampleStudent.setUsername("charlie");
        sampleStudent.setPasswordHash("$2a$hashed");
        sampleStudent.setRole(Role.STUDENT);
        sampleStudent.setName("Charlie Brown");
        sampleStudent.setDob(LocalDate.of(2000, 5, 15));
        sampleStudent.setGraduationDate(LocalDate.of(2028, 6, 15));
        sampleStudent.setStatus(User.UserStatus.ACTIVE);

        // 创建示例学生请求
        sampleStudentRequest = new UserRequest();
        sampleStudentRequest.setUsername("charlie");
        sampleStudentRequest.setPassword("password123");
        sampleStudentRequest.setRole(Role.STUDENT);
        sampleStudentRequest.setName("Charlie Brown");
        sampleStudentRequest.setDob(LocalDate.of(2000, 5, 15));
        sampleStudentRequest.setGraduationDate(LocalDate.of(2028, 6, 15));
    }

    // ==================== create ====================

    @Test
    @DisplayName("创建用户 - 有效数据成功")
    void create_validRequest_returnsUserDTO() {
        // Arrange
        when(userRepository.findByUsername("charlie")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("$2a$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });

        // Act
        UserDTO result = userService.create(sampleStudentRequest);

        // Assert
        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("charlie", result.getUsername());
        assertEquals(Role.STUDENT, result.getRole());
        assertEquals("Charlie Brown", result.getName());
        assertEquals(User.UserStatus.ACTIVE.name(), result.getStatus());
        verify(auditService).recordUserCreate(isNull(), eq(10L), eq("charlie"), eq("STUDENT"));
    }

    @Test
    @DisplayName("创建用户 - 用户名重复抛出冲突异常")
    void create_duplicateUsername_throwsConflict() {
        // Arrange
        when(userRepository.findByUsername("charlie")).thenReturn(Optional.of(sampleStudent));

        // Act & Assert
        ApiException ex = assertThrows(ApiException.class, () ->
                userService.create(sampleStudentRequest));
        assertEquals(409, ex.getStatus());
        assertTrue(ex.getMessage().contains("用户名已存在"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("创建用户 - 教授未指定院系抛出异常")
    void create_professorWithoutDepartment_throwsBadRequest() {
        // Arrange
        UserRequest profReq = new UserRequest();
        profReq.setUsername("prof_smith");
        profReq.setPassword("pass123");
        profReq.setRole(Role.PROFESSOR);
        profReq.setName("Bob Smith");
        profReq.setDepartment(null); // 未指定院系

        when(userRepository.findByUsername("prof_smith")).thenReturn(Optional.empty());

        // Act & Assert
        ApiException ex = assertThrows(ApiException.class, () ->
                userService.create(profReq));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("院系"));
    }

    @Test
    @DisplayName("创建用户 - 学生未指定毕业日期抛出异常")
    void create_studentWithoutGraduationDate_throwsBadRequest() {
        // Arrange
        UserRequest studReq = new UserRequest();
        studReq.setUsername("newstudent");
        studReq.setPassword("pass123");
        studReq.setRole(Role.STUDENT);
        studReq.setName("New Student");
        studReq.setGraduationDate(null); // 未指定毕业日期

        when(userRepository.findByUsername("newstudent")).thenReturn(Optional.empty());

        // Act & Assert
        ApiException ex = assertThrows(ApiException.class, () ->
                userService.create(studReq));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("毕业日期"));
    }

    // ==================== getById ====================

    @Test
    @DisplayName("按 ID 查询 - 存在时返回 UserDTO")
    void getById_existingId_returnsUserDTO() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleStudent));

        // Act
        UserDTO result = userService.getById(1L);

        // Assert
        assertEquals(1L, result.getId());
        assertEquals("charlie", result.getUsername());
        assertEquals("Charlie Brown", result.getName());
    }

    @Test
    @DisplayName("按 ID 查询 - 不存在时抛出 404")
    void getById_nonExistentId_throwsNotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ApiException ex = assertThrows(ApiException.class, () ->
                userService.getById(999L));
        assertEquals(404, ex.getStatus());
        assertTrue(ex.getMessage().contains("用户不存在"));
    }

    // ==================== delete ====================

    @Test
    @DisplayName("删除用户 - INACTIVE 用户成功删除")
    void delete_inactiveUser_succeeds() {
        // Arrange
        sampleStudent.setStatus(User.UserStatus.INACTIVE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleStudent));

        // Act
        userService.delete(1L);

        // Assert
        verify(userRepository).delete(sampleStudent);
        verify(auditService).recordUserDelete(isNull(), eq(1L), eq("charlie"));
    }

    @Test
    @DisplayName("删除用户 - ACTIVE 用户抛出冲突异常")
    void delete_activeUser_throwsConflict() {
        // Arrange
        sampleStudent.setStatus(User.UserStatus.ACTIVE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleStudent));

        // Act & Assert
        ApiException ex = assertThrows(ApiException.class, () ->
                userService.delete(1L));
        assertEquals(409, ex.getStatus());
        assertTrue(ex.getMessage().contains("ACTIVE"));
        verify(userRepository, never()).delete(any());
    }

    @Test
    @DisplayName("删除用户 - 不存在的用户抛出 404")
    void delete_nonExistentUser_throwsNotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ApiException ex = assertThrows(ApiException.class, () ->
                userService.delete(999L));
        assertEquals(404, ex.getStatus());
    }

    // ==================== getAllUsers ====================

    @Test
    @DisplayName("查询所有用户 - 按角色过滤")
    void getAllUsers_withRoleFilter_filtersCorrectly() {
        // Arrange
        List<User> students = List.of(sampleStudent);
        when(userRepository.findByRole(Role.STUDENT)).thenReturn(students);

        // Act
        List<UserDTO> result = userService.getAllUsers(Role.STUDENT);

        // Assert
        assertEquals(1, result.size());
        assertEquals("charlie", result.get(0).getUsername());
        verify(userRepository).findByRole(Role.STUDENT);
        verify(userRepository, never()).findAll();
    }

    @Test
    @DisplayName("查询所有用户 - 无过滤时返回全部")
    void getAllUsers_withoutFilter_returnsAll() {
        // Arrange
        User prof = new User();
        prof.setId(2L);
        prof.setUsername("bob");
        prof.setRole(Role.PROFESSOR);
        prof.setName("Bob Smith");
        prof.setStatus(User.UserStatus.ACTIVE);

        when(userRepository.findAll()).thenReturn(List.of(sampleStudent, prof));

        // Act
        List<UserDTO> result = userService.getAllUsers(null);

        // Assert
        assertEquals(2, result.size());
        verify(userRepository).findAll();
        verify(userRepository, never()).findByRole(any());
    }

    // ==================== update ====================

    @Test
    @DisplayName("更新用户 - 修改字段成功")
    void update_existingUser_modifiesFields() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleStudent));
        when(passwordEncoder.encode("newpass")).thenReturn("$2a$newhash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserRequest updateReq = new UserRequest();
        updateReq.setUsername("charlie_updated");
        updateReq.setPassword("newpass");
        updateReq.setRole(Role.STUDENT);
        updateReq.setName("Charlie Brown Updated");
        updateReq.setGraduationDate(LocalDate.of(2029, 6, 15));

        // Act
        UserDTO result = userService.update(1L, updateReq);

        // Assert
        assertEquals("charlie_updated", result.getUsername());
        assertEquals("Charlie Brown Updated", result.getName());
        verify(auditService).recordUserUpdate(isNull(), eq(1L), eq("charlie_updated"));
    }
}
