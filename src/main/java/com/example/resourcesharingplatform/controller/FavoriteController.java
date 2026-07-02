package com.example.resourcesharingplatform.controller;

import com.example.resourcesharingplatform.dto.Result;
import com.example.resourcesharingplatform.entity.Favorite;
import com.example.resourcesharingplatform.entity.User;
import com.example.resourcesharingplatform.repository.FavoriteRepository;
import com.example.resourcesharingplatform.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 收藏控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteRepository favoriteRepository;
    private final ResourceRepository resourceRepository;

    @PostMapping("/toggle/{resourceId}")
    @Transactional
    public Result<Map<String, Object>> toggleFavorite(@PathVariable Long resourceId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        Long userId = currentUser.getId();

        boolean isFavorited = favoriteRepository.existsByUserIdAndResourceId(userId, resourceId);

        if (isFavorited) {
            favoriteRepository.deleteByUserIdAndResourceId(userId, resourceId);
            // 更新收藏数
            long count = favoriteRepository.countByResourceId(resourceId);
            resourceRepository.updateFavoriteCount(resourceId, count);
            return Result.success(Map.of("isFavorite", false, "count", count));
        } else {
            Favorite favorite = Favorite.builder()
                    .userId(userId)
                    .resourceId(resourceId)
                    .build();
            favoriteRepository.save(favorite);
            // 更新收藏数
            long count = favoriteRepository.countByResourceId(resourceId);
            resourceRepository.updateFavoriteCount(resourceId, count);
            return Result.success(Map.of("isFavorite", true, "count", count));
        }
    }

    @GetMapping("/status/{resourceId}")
    public Result<Map<String, Object>> getFavoriteStatus(@PathVariable Long resourceId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        Long userId = currentUser.getId();

        boolean isFavorite = favoriteRepository.existsByUserIdAndResourceId(userId, resourceId);
        long count = favoriteRepository.countByResourceId(resourceId);

        return Result.success(Map.of("isFavorite", isFavorite, "count", count));
    }

    @GetMapping("/my")
    public Result<List<Long>> getMyFavorites() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        List<Long> resourceIds = favoriteRepository.findResourceIdsByUserId(currentUser.getId());
        return Result.success(resourceIds);
    }
}
