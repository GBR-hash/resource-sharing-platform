package com.example.resourcesharingplatform.repository;

import com.example.resourcesharingplatform.entity.CompetitionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 竞赛类型数据访问接口
 */
@Repository
public interface CompetitionTypeRepository extends JpaRepository<CompetitionType, Long> {

    Optional<CompetitionType> findByName(String name);

    List<CompetitionType> findByStatus(Integer status);

    List<CompetitionType> findByStatusOrderBySortOrderAsc(Integer status);

    boolean existsByName(String name);
}