package com.example.resourcesharingplatform.controller;

import com.example.resourcesharingplatform.dto.Result;
import com.example.resourcesharingplatform.dto.user.UserDTO;
import com.example.resourcesharingplatform.dto.user.UserUpdateRequest;
import com.example.resourcesharingplatform.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public Result<UserDTO> getUserById(@PathVariable Long id) {
        UserDTO user = userService.getUserById(id);
        return Result.success(user);
    }

    @PutMapping("/{id}")
    public Result<UserDTO> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        UserDTO user = userService.updateUser(id, request);
        return Result.success("更新成功", user);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success("删除成功");
    }
}