package com.example.resourcesharingplatform.controller;

import com.example.resourcesharingplatform.dto.Result;
import com.example.resourcesharingplatform.entity.Category;
import com.example.resourcesharingplatform.entity.CompetitionType;
import com.example.resourcesharingplatform.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公开接口控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final CategoryService categoryService;

    @GetMapping("/categories")
    public Result<List<Category>> listCategories() {
        List<Category> categories = categoryService.listCategories();
        return Result.success(categories);
    }

    @GetMapping("/competition-types")
    public Result<List<CompetitionType>> listCompetitionTypes() {
        List<CompetitionType> types = categoryService.listCompetitionTypes();
        return Result.success(types);
    }
}