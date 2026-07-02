package com.example.resourcesharingplatform.controller;

import com.example.resourcesharingplatform.dto.PageResponse;
import com.example.resourcesharingplatform.dto.Result;
import com.example.resourcesharingplatform.dto.resource.ResourceDTO;
import com.example.resourcesharingplatform.dto.resource.ResourceQueryRequest;
import com.example.resourcesharingplatform.dto.user.UserDTO;
import com.example.resourcesharingplatform.entity.Category;
import com.example.resourcesharingplatform.entity.CompetitionType;
import com.example.resourcesharingplatform.repository.ResourceRepository;
import com.example.resourcesharingplatform.repository.UserRepository;
import com.example.resourcesharingplatform.service.CategoryService;
import com.example.resourcesharingplatform.service.ResourceService;
import com.example.resourcesharingplatform.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final ResourceService resourceService;
    private final CategoryService categoryService;
    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;

    @GetMapping("/users")
    public Result<PageResponse<UserDTO>> listUsers(
            @RequestParam(value = "role", required = false) Integer role,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "pageNumber", defaultValue = "0") Integer pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {

        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<UserDTO> users = userService.listUsers(role, status, pageable);
        return Result.success(PageResponse.from(users));
    }

    @PutMapping("/users/{id}/status")
    public Result<Void> updateUserStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {
        userService.updateUserStatus(id, status);
        return Result.success("状态更新成功");
    }

    @PutMapping("/users/{id}/role")
    public Result<Void> updateUserRole(
            @PathVariable Long id,
            @RequestParam Integer role) {
        userService.updateUserRole(id, role);
        return Result.success("角色更新成功");
    }

    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success("用户删除成功");
    }

    @GetMapping("/resources")
    public Result<PageResponse<ResourceDTO>> listResources(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "competitionTypeId", required = false) Long competitionTypeId,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "pageNumber", defaultValue = "0") Integer pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {

        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ResourceDTO> resources;

        if (keyword != null && !keyword.isEmpty()) {
            resources = resourceRepository.findByTitleContainingOrDescriptionContaining(keyword, keyword, pageable)
                    .map(resourceService::convertToDTO);
        } else if (status != null) {
            resources = resourceRepository.findByStatus(status, pageable).map(resourceService::convertToDTO);
        } else {
            // 管理员查看所有资料，包括审核中的
            resources = resourceRepository.findAll(pageable).map(resourceService::convertToDTO);
        }

        return Result.success(PageResponse.from(resources));
    }

    @PutMapping("/resources/{id}/approve")
    public Result<Void> approveResource(@PathVariable Long id) {
        Long adminId = getCurrentAdminId();
        resourceService.approveResource(id, adminId);
        return Result.success("审核通过");
    }

    @PutMapping("/resources/{id}/reject")
    public Result<Void> rejectResource(@PathVariable Long id) {
        Long adminId = getCurrentAdminId();
        resourceService.rejectResource(id, adminId);
        return Result.success("已拒绝");
    }

    @DeleteMapping("/resources/{id}")
    public Result<Void> deleteResource(@PathVariable Long id) {
        resourceService.deleteResource(id);
        return Result.success("资料删除成功");
    }

    @PostMapping("/categories")
    public Result<Category> createCategory(@RequestBody Category category) {
        Category created = categoryService.createCategory(category);
        return Result.success("创建成功", created);
    }

    @PutMapping("/categories/{id}")
    public Result<Category> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        Category updated = categoryService.updateCategory(id, category);
        return Result.success("更新成功", updated);
    }

    @DeleteMapping("/categories/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success("删除成功");
    }

    @GetMapping("/categories")
    public Result<List<Category>> listCategories() {
        List<Category> categories = categoryService.listCategories();
        return Result.success(categories);
    }

    @PostMapping("/competition-types")
    public Result<CompetitionType> createCompetitionType(@RequestBody CompetitionType type) {
        CompetitionType created = categoryService.createCompetitionType(type);
        return Result.success("创建成功", created);
    }

    @PutMapping("/competition-types/{id}")
    public Result<CompetitionType> updateCompetitionType(@PathVariable Long id, @RequestBody CompetitionType type) {
        CompetitionType updated = categoryService.updateCompetitionType(id, type);
        return Result.success("更新成功", updated);
    }

    @DeleteMapping("/competition-types/{id}")
    public Result<Void> deleteCompetitionType(@PathVariable Long id) {
        categoryService.deleteCompetitionType(id);
        return Result.success("删除成功");
    }

    @GetMapping("/competition-types")
    public Result<List<CompetitionType>> listCompetitionTypes() {
        List<CompetitionType> types = categoryService.listCompetitionTypes();
        return Result.success(types);
    }

    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("userCount", userRepository.count());
        stats.put("resourceCount", resourceRepository.count());
        stats.put("downloadCount", resourceRepository.getTotalDownloadCount());
        stats.put("pendingCount", resourceRepository.countByStatus(0));
        stats.put("approvedCount", resourceRepository.countByStatus(1));
        stats.put("rejectedCount", resourceRepository.countByStatus(2));
        return Result.success(stats);
    }

    private Long getCurrentAdminId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username).map(u -> u.getId()).orElse(1L);
    }
}