package com.example.resourcesharingplatform.repository;

import com.example.resourcesharingplatform.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 分类数据访问接口
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);

    List<Category> findByParentId(Long parentId);

    List<Category> findByStatus(Integer status);

    List<Category> findByStatusOrderBySortOrderAsc(Integer status);

    boolean existsByName(String name);
}