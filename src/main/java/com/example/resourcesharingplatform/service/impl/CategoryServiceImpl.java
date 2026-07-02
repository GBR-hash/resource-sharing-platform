package com.example.resourcesharingplatform.service.impl;

import com.example.resourcesharingplatform.entity.Category;
import com.example.resourcesharingplatform.entity.CompetitionType;
import com.example.resourcesharingplatform.exception.BusinessException;
import com.example.resourcesharingplatform.repository.CategoryRepository;
import com.example.resourcesharingplatform.repository.CompetitionTypeRepository;
import com.example.resourcesharingplatform.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 分类服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CompetitionTypeRepository competitionTypeRepository;

    @Override
    @Transactional
    public Category createCategory(Category category) {
        if (categoryRepository.existsByName(category.getName())) {
            throw new BusinessException("分类名称已存在");
        }
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public Category updateCategory(Long id, Category category) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("分类不存在"));

        if (!existing.getName().equals(category.getName()) && 
            categoryRepository.existsByName(category.getName())) {
            throw new BusinessException("分类名称已存在");
        }

        existing.setName(category.getName());
        existing.setDescription(category.getDescription());
        existing.setParentId(category.getParentId());
        existing.setSortOrder(category.getSortOrder());
        existing.setStatus(category.getStatus());

        return categoryRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new BusinessException("分类不存在");
        }
        categoryRepository.deleteById(id);
    }

    @Override
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("分类不存在"));
    }

    @Override
    public List<Category> listCategories() {
        return categoryRepository.findByStatusOrderBySortOrderAsc(1);
    }

    @Override
    @Transactional
    public CompetitionType createCompetitionType(CompetitionType type) {
        if (competitionTypeRepository.existsByName(type.getName())) {
            throw new BusinessException("竞赛类型名称已存在");
        }
        return competitionTypeRepository.save(type);
    }

    @Override
    @Transactional
    public CompetitionType updateCompetitionType(Long id, CompetitionType type) {
        CompetitionType existing = competitionTypeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("竞赛类型不存在"));

        if (!existing.getName().equals(type.getName()) && 
            competitionTypeRepository.existsByName(type.getName())) {
            throw new BusinessException("竞赛类型名称已存在");
        }

        existing.setName(type.getName());
        existing.setDescription(type.getDescription());
        existing.setSortOrder(type.getSortOrder());
        existing.setStatus(type.getStatus());

        return competitionTypeRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteCompetitionType(Long id) {
        if (!competitionTypeRepository.existsById(id)) {
            throw new BusinessException("竞赛类型不存在");
        }
        competitionTypeRepository.deleteById(id);
    }

    @Override
    public CompetitionType getCompetitionTypeById(Long id) {
        return competitionTypeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("竞赛类型不存在"));
    }

    @Override
    public List<CompetitionType> listCompetitionTypes() {
        return competitionTypeRepository.findByStatusOrderBySortOrderAsc(1);
    }
}