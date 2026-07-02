package com.example.resourcesharingplatform.service;

import com.example.resourcesharingplatform.dto.user.LoginRequest;
import com.example.resourcesharingplatform.dto.user.RegisterRequest;
import com.example.resourcesharingplatform.dto.user.UserDTO;
import com.example.resourcesharingplatform.dto.user.UserUpdateRequest;
import com.example.resourcesharingplatform.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

/**
 * 用户服务接口
 */
public interface UserService {

    User register(RegisterRequest request);

    Map<String, Object> login(LoginRequest request);

    UserDTO getCurrentUser(String username);

    UserDTO updateUser(Long id, UserUpdateRequest request);

    void updateUserStatus(Long id, Integer status);

    void updateUserRole(Long id, Integer role);

    void deleteUser(Long id);

    Page<UserDTO> listUsers(Integer role, Integer status, Pageable pageable);

    UserDTO getUserById(Long id);
}