package com.example.resourcesharingplatform.service.impl;

import com.example.resourcesharingplatform.dto.user.LoginRequest;
import com.example.resourcesharingplatform.dto.user.RegisterRequest;
import com.example.resourcesharingplatform.dto.user.UserDTO;
import com.example.resourcesharingplatform.dto.user.UserUpdateRequest;
import com.example.resourcesharingplatform.entity.User;
import com.example.resourcesharingplatform.exception.BusinessException;
import com.example.resourcesharingplatform.repository.UserRepository;
import com.example.resourcesharingplatform.service.UserService;
import com.example.resourcesharingplatform.util.Constants;
import com.example.resourcesharingplatform.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("用户名已存在");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("邮箱已被注册");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .phone(request.getPhone())
                .realName(request.getRealName())
                .role(Constants.ROLE_USER)
                .status(Constants.USER_STATUS_ACTIVE)
                .build();

        return userRepository.save(user);
    }

    @Override
    public Map<String, Object> login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        if (user.getStatus() != Constants.USER_STATUS_ACTIVE) {
            throw new BusinessException("用户已被禁用");
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", UserDTO.from(user));
        return result;
    }

    @Override
    public UserDTO getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        return UserDTO.from(user);
    }

    @Override
    @Transactional
    public UserDTO updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (!request.getEmail().equals(user.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
                throw new BusinessException("邮箱已被使用");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        if (request.getRealName() != null) {
            user.setRealName(request.getRealName());
        }

        User updatedUser = userRepository.save(user);
        return UserDTO.from(updatedUser);
    }

    @Override
    @Transactional
    public void updateUserStatus(Long id, Integer status) {
        int affected = userRepository.updateStatus(id, status);
        if (affected == 0) {
            throw new BusinessException("用户不存在");
        }
    }

    @Override
    @Transactional
    public void updateUserRole(Long id, Integer role) {
        int affected = userRepository.updateRole(id, role);
        if (affected == 0) {
            throw new BusinessException("用户不存在");
        }
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new BusinessException("用户不存在");
        }
        userRepository.deleteById(id);
    }

    @Override
    public Page<UserDTO> listUsers(Integer role, Integer status, Pageable pageable) {
        Page<User> users;

        if (role != null && status != null) {
            users = userRepository.findAll(pageable);
            List<UserDTO> filtered = users.getContent().stream()
                    .filter(u -> u.getRole().equals(role) && u.getStatus().equals(status))
                    .map(UserDTO::from)
                    .collect(Collectors.toList());
            return new PageImpl<>(filtered, pageable, users.getTotalElements());
        } else if (role != null) {
            return userRepository.findByRole(role, pageable).map(UserDTO::from);
        } else if (status != null) {
            return userRepository.findByStatus(status, pageable).map(UserDTO::from);
        }
        return userRepository.findAll(pageable).map(UserDTO::from);
    }

    @Override
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        return UserDTO.from(user);
    }
}