package com.example.resourcesharingplatform.service;

import com.example.resourcesharingplatform.entity.Category;
import com.example.resourcesharingplatform.entity.CompetitionType;

import java.util.List;

/**
 * 分类服务接口
 */
public interface CategoryService {

    Category createCategory(Category category);

    Category updateCategory(Long id, Category category);

    void deleteCategory(Long id);

    Category getCategoryById(Long id);

    List<Category> listCategories();

    CompetitionType createCompetitionType(CompetitionType type);

    CompetitionType updateCompetitionType(Long id, CompetitionType type);

    void deleteCompetitionType(Long id);

    CompetitionType getCompetitionTypeById(Long id);

    List<CompetitionType> listCompetitionTypes();
}