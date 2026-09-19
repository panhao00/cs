package com.crs.repository;

import com.crs.domain.Role;
import com.crs.domain.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserRepository 数据访问层测试。
 * <p>
 * 使用 @DataJpaTest 启动切片测试，自动配置 H2 内存数据库和 JPA Repository，
 * 不加载完整的 Spring 容器。
 */
@DataJpaTest
@ActiveProfiles("dev")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User student;
    private User professor;
    private User registrar;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        student = new User();
        student.setUsername("student1");
        student.setPasswordHash("hashed_password_1");
        student.setRole(Role.STUDENT);
        student.setName("张三");
        student.setStatus(User.UserStatus.ACTIVE);

        professor = new User();
        professor.setUsername("prof1");
        professor.setPasswordHash("hashed_password_2");
        professor.setRole(Role.PROFESSOR);
        professor.setName("李教授");
        professor.setDepartment("Computer Science");
        professor.setStatus(User.UserStatus.ACTIVE);

        registrar = new User();
        registrar.setUsername("registrar1");
        registrar.setPasswordHash("hashed_password_3");
        registrar.setRole(Role.REGISTRAR);
        registrar.setName("王注册主任");
        registrar.setStatus(User.UserStatus.ACTIVE);

        userRepository.saveAll(List.of(student, professor, registrar));
    }

    // ==================== save 测试 ====================

    @Test
    @DisplayName("保存用户 - 正确持久化所有字段")
    void saveUser_persistsAllFields() {
        User newUser = new User();
        newUser.setUsername("new_student");
        newUser.setPasswordHash("hash_new");
        newUser.setRole(Role.STUDENT);
        newUser.setName("赵六");

        User saved = userRepository.save(newUser);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUsername()).isEqualTo("new_student");
        assertThat(saved.getRole()).isEqualTo(Role.STUDENT);
        assertThat(saved.getName()).isEqualTo("赵六");
        assertThat(saved.getStatus()).isEqualTo(User.UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("保存用户 - id 自增")
    void saveUser_autoGeneratesId() {
        User user1 = createUser("user_a", Role.STUDENT, "A");
        User user2 = createUser("user_b", Role.STUDENT, "B");

        User saved1 = userRepository.save(user1);
        User saved2 = userRepository.save(user2);

        assertThat(saved2.getId()).isGreaterThan(saved1.getId());
    }

    // ==================== findByUsername 测试 ====================

    @Test
    @DisplayName("findByUsername - 用户存在时返回 Optional 包含用户")
    void findByUsername_existingUser_returnsUser() {
        Optional<User> found = userRepository.findByUsername("student1");

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("student1");
        assertThat(found.get().getName()).isEqualTo("张三");
        assertThat(found.get().getRole()).isEqualTo(Role.STUDENT);
    }

    @Test
    @DisplayName("findByUsername - 用户不存在时返回空 Optional")
    void findByUsername_nonExistingUser_returnsEmpty() {
        Optional<User> found = userRepository.findByUsername("nonexistent");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findByUsername - 查找不同角色的用户")
    void findByUsername_differentRoles_returnsCorrectUser() {
        Optional<User> foundProfessor = userRepository.findByUsername("prof1");
        Optional<User> foundRegistrar = userRepository.findByUsername("registrar1");

        assertThat(foundProfessor).isPresent();
        assertThat(foundProfessor.get().getRole()).isEqualTo(Role.PROFESSOR);

        assertThat(foundRegistrar).isPresent();
        assertThat(foundRegistrar.get().getRole()).isEqualTo(Role.REGISTRAR);
    }

    // ==================== existsByUsername 测试 ====================

    @Test
    @DisplayName("existsByUsername - 用户存在返回 true")
    void existsByUsername_existingUser_returnsTrue() {
        boolean exists = userRepository.existsByUsername("student1");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByUsername - 用户不存在返回 false")
    void existsByUsername_nonExistingUser_returnsFalse() {
        boolean exists = userRepository.existsByUsername("nonexistent");

        assertThat(exists).isFalse();
    }

    // ==================== findByRole 测试 ====================

    @Test
    @DisplayName("findByRole - 查询所有学生用户")
    void findByRole_studentRole_returnsAllStudents() {
        // 额外添加一个学生
        User extraStudent = createUser("student2", Role.STUDENT, "李四");
        userRepository.save(extraStudent);

        List<User> students = userRepository.findByRole(Role.STUDENT);

        assertThat(students).hasSizeGreaterThanOrEqualTo(2);
        assertThat(students).allMatch(u -> u.getRole() == Role.STUDENT);
    }

    @Test
    @DisplayName("findByRole - 查询教授用户")
    void findByRole_professorRole_returnsAllProfessors() {
        List<User> professors = userRepository.findByRole(Role.PROFESSOR);

        assertThat(professors).hasSize(1);
        assertThat(professors.get(0).getUsername()).isEqualTo("prof1");
        assertThat(professors.get(0).getDepartment()).isEqualTo("Computer Science");
    }

    @Test
    @DisplayName("findByRole - 查询注册主任用户")
    void findByRole_registrarRole_returnsAllRegistrars() {
        List<User> registrars = userRepository.findByRole(Role.REGISTRAR);

        assertThat(registrars).hasSize(1);
        assertThat(registrars.get(0).getUsername()).isEqualTo("registrar1");
    }

    @Test
    @DisplayName("findByRole - 无匹配用户时返回空列表")
    void findByRole_noMatchingRole_returnsEmptyList() {
        userRepository.deleteAll();

        List<User> result = userRepository.findByRole(Role.STUDENT);

        assertThat(result).isEmpty();
    }

    // ==================== JpaRepository 继承方法测试 ====================

    @Test
    @DisplayName("findById - 存在的用户返回 Optional 包含用户")
    void findById_existingUser_returnsUser() {
        Optional<User> found = userRepository.findById(student.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("student1");
    }

    @Test
    @DisplayName("findById - 不存在的 id 返回空 Optional")
    void findById_nonExistingId_returnsEmpty() {
        Optional<User> found = userRepository.findById(99999L);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findAll - 返回所有保存的用户")
    void findAll_returnsAllUsers() {
        List<User> all = userRepository.findAll();

        assertThat(all).hasSize(3);
    }

    @Test
    @DisplayName("deleteById - 删除后数据库中不再存在该用户")
    void deleteById_removesUser() {
        Long id = student.getId();

        userRepository.deleteById(id);

        assertThat(userRepository.findById(id)).isEmpty();
        assertThat(userRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("count - 返回正确的用户总数")
    void count_returnsCorrectCount() {
        long count = userRepository.count();

        assertThat(count).isEqualTo(3);
    }

    // ==================== 边界场景测试 ====================

    @Test
    @DisplayName("username 唯一约束 - 保存重复 username 抛出异常")
    void saveDuplicateUsername_throwsException() {
        User duplicate = createUser("student1", Role.STUDENT, "重复用户");

        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> userRepository.save(duplicate)
        );
    }

    @Test
    @DisplayName("update - 修改用户字段后持久化")
    void updateUser_fieldsPersist() {
        student.setName("张三（已改名）");
        student.setDepartment("Mathematics");

        userRepository.save(student);

        Optional<User> found = userRepository.findById(student.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("张三（已改名）");
        assertThat(found.get().getDepartment()).isEqualTo("Mathematics");
    }

    // ==================== 辅助方法 ====================

    private User createUser(String username, Role role, String name) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash("hashed_" + username);
        user.setRole(role);
        user.setName(name);
        return user;
    }
}
