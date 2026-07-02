package com.example.resourcesharingplatform.repository;

import com.example.resourcesharingplatform.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户数据访问接口
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Page<User> findByRole(Integer role, Pageable pageable);

    Page<User> findByStatus(Integer status, Pageable pageable);

    @Modifying
    @Query("UPDATE User u SET u.status = :status WHERE u.id = :id")
    int updateStatus(Long id, Integer status);

    @Modifying
    @Query("UPDATE User u SET u.role = :role WHERE u.id = :id")
    int updateRole(Long id, Integer role);
}