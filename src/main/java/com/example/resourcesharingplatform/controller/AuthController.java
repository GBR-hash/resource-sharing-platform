package com.example.resourcesharingplatform.controller;

import com.example.resourcesharingplatform.dto.Result;
import com.example.resourcesharingplatform.dto.user.LoginRequest;
import com.example.resourcesharingplatform.dto.user.RegisterRequest;
import com.example.resourcesharingplatform.dto.user.UserDTO;
import com.example.resourcesharingplatform.entity.User;
import com.example.resourcesharingplatform.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<UserDTO> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request);
        return Result.success("注册成功", UserDTO.from(user));
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        Map<String, Object> result = userService.login(request);
        return Result.success("登录成功", result);
    }

    @GetMapping("/me")
    public Result<UserDTO> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        UserDTO user = userService.getCurrentUser(username);
        return Result.success(user);
    }
}