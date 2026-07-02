package com.example.resourcesharingplatform.repository;

import com.example.resourcesharingplatform.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 收藏数据访问接口
 */
@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Optional<Favorite> findByUserIdAndResourceId(Long userId, Long resourceId);

    boolean existsByUserIdAndResourceId(Long userId, Long resourceId);

    void deleteByUserIdAndResourceId(Long userId, Long resourceId);

    List<Favorite> findByUserId(Long userId);

    @Query("SELECT COUNT(f) FROM Favorite f WHERE f.resourceId = :resourceId")
    long countByResourceId(Long resourceId);

    @Query("SELECT f.resourceId FROM Favorite f WHERE f.userId = :userId")
    List<Long> findResourceIdsByUserId(Long userId);
}
