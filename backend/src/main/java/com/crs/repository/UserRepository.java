package com.crs.repository;

import com.crs.domain.Role;
import com.crs.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 用户仓储。
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** 按用户名查找(LoadUserByUsername 使用) */
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    /** 按角色查找(注册主任维护用户用例用) */
    List<User> findByRole(Role role);
}
