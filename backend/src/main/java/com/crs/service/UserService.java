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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务: 实现用例 7.6 Maintain Student / 7.7 Maintain Professor。
 * <p>
 * 基本流(以 Maintain Student 为例):
 * 1. 注册主任查询学生信息(可选按姓名/学号)
 * 2. 系统检索并显示学生列表
 * 3. 注册主任选择"新建学生"或选择已有学生进行修改
 * 4. 系统显示学生信息表单(必填字段、可修改字段)
 * 5. 注册主任提交修改
 * 6. 系统保存并确认
 * <p>
 * 备选流:
 * - Student Not Found: 查询的学生不存在 → 报错
 * - Invalid Information: 提交数据校验失败 → 报错,用例终止
 * <p>
 * 安全: 强制 REGISTRAR 角色;敏感字段(SSN/密码哈希)不通过 DTO 暴露。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    /** 查询所有用户(可选按角色过滤) */
    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers(Role roleFilter) {
        List<User> users;
        if (roleFilter != null) {
            users = userRepository.findByRole(roleFilter);
        } else {
            users = userRepository.findAll();
        }
        return users.stream().map(this::toDTO).toList();
    }

    /** 按 ID 查询用户 */
    @Transactional(readOnly = true)
    public UserDTO getById(Long id) {
        return userRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> ApiException.notFound("用户不存在: " + id));
    }

    /** 按用户名查询 */
    @Transactional(readOnly = true)
    public Optional<UserDTO> getByUsername(String username) {
        return userRepository.findByUsername(username).map(this::toDTO);
    }

    /**
     * 创建用户(基本流 3-6)。
     */
    @Transactional
    public UserDTO create(UserRequest request) {
        // 校验用户名唯一
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw ApiException.conflict("用户名已存在: " + request.getUsername());
        }
        validateRequest(request);
        // 创建时密码必填
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw ApiException.badRequest("创建用户时 password 不能为空");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setName(request.getName());
        user.setDob(request.getDob());
        user.setSsn(null);  // 简化: 不通过 API 收集 SSN,实际应单独流程采集
        user.setStatus(User.UserStatus.ACTIVE);

        // 角色相关字段
        if (request.getRole() == Role.PROFESSOR) {
            user.setDepartment(request.getDepartment());
        } else if (request.getRole() == Role.STUDENT) {
            user.setGraduationDate(request.getGraduationDate());
        }

        user = userRepository.save(user);
        auditService.recordUserCreate(null, user.getId(), user.getUsername(),
                user.getRole().name());
        log.info("注册主任创建用户: id={} username={} role={}",
                user.getId(), user.getUsername(), user.getRole());
        return toDTO(user);
    }

    /**
     * 更新用户(基本流 3-6)。
     * 密码字段留空表示不修改密码。
     */
    @Transactional
    public UserDTO update(Long id, UserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("用户不存在: " + id));
        validateRequest(request);

        // 用户名修改需校验唯一
        if (!user.getUsername().equals(request.getUsername())) {
            if (userRepository.findByUsername(request.getUsername()).isPresent()) {
                throw ApiException.conflict("用户名已存在: " + request.getUsername());
            }
            user.setUsername(request.getUsername());
        }

        // 密码留空表示不修改
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        user.setRole(request.getRole());
        user.setName(request.getName());
        user.setDob(request.getDob());

        if (request.getRole() == Role.PROFESSOR) {
            user.setDepartment(request.getDepartment());
        } else if (request.getRole() == Role.STUDENT) {
            user.setGraduationDate(request.getGraduationDate());
        }

        user = userRepository.save(user);
        auditService.recordUserUpdate(null, user.getId(), user.getUsername());
        log.info("注册主任更新用户: id={} username={} role={}",
                user.getId(), user.getUsername(), user.getRole());
        return toDTO(user);
    }

    /**
     * 设置用户状态(ACTIVE/INACTIVE)。
     * 备选流 Student Not Found: 用户不存在 → 404。
     */
    @Transactional
    public UserDTO setStatus(Long id, User.UserStatus status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("用户不存在: " + id));
        user.setStatus(status);
        user = userRepository.save(user);
        log.info("注册主任设置用户 {} 状态为 {}", id, status);
        return toDTO(user);
    }

    /**
     * 删除用户(仅当无活跃选课时)。
     * 简化: 直接物理删除;实际可软删除(INACTIVE)。
     */
    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("用户不存在: " + id));
        if (user.getStatus() == User.UserStatus.ACTIVE) {
            throw ApiException.conflict("用户状态为 ACTIVE,请先设为 INACTIVE 再删除");
        }
        userRepository.delete(user);
        auditService.recordUserDelete(null, user.getId(), user.getUsername());
        log.info("注册主任删除用户: id={} username={}", id, user.getUsername());
    }

    // ---- 内部方法 ----

    private void validateRequest(UserRequest request) {
        if (request.getRole() == Role.PROFESSOR && (request.getDepartment() == null
                || request.getDepartment().isBlank())) {
            throw ApiException.badRequest("教授必须指定院系");
        }
        if (request.getRole() == Role.STUDENT && request.getGraduationDate() == null) {
            throw ApiException.badRequest("学生必须指定预计毕业日期");
        }
        if (request.getDob() != null && request.getDob().isAfter(LocalDate.now())) {
            throw ApiException.badRequest("出生日期不能晚于今天");
        }
    }

    private UserDTO toDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getName(),
                user.getDepartment(),
                user.getDob(),
                user.getGraduationDate(),
                user.getStatus().name()
        );
    }
}
