package com.example.resourcesharingplatform.repository;

import com.example.resourcesharingplatform.entity.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 资料数据访问接口
 */
@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {

    Page<Resource> findByStatus(Integer status, Pageable pageable);

    Page<Resource> findByCategoryId(Long categoryId, Pageable pageable);

    Page<Resource> findByCompetitionTypeId(Long competitionTypeId, Pageable pageable);

    Page<Resource> findByUploaderId(Long uploaderId, Pageable pageable);

    Page<Resource> findByTitleContainingOrDescriptionContaining(String title, String description, Pageable pageable);

    Page<Resource> findByCategoryIdAndStatus(Long categoryId, Integer status, Pageable pageable);

    Page<Resource> findByCompetitionTypeIdAndStatus(Long competitionTypeId, Integer status, Pageable pageable);

    Page<Resource> findByStatusOrderByCreatedAtDesc(Integer status, Pageable pageable);

    @Query("SELECT r FROM Resource r WHERE r.status = :status AND r.createdAt BETWEEN :startTime AND :endTime")
    Page<Resource> findByStatusAndTimeRange(Integer status, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    @Modifying
    @Query("UPDATE Resource r SET r.status = :status, r.approvedAt = :approvedAt, r.approvedBy = :approvedBy WHERE r.id = :id")
    int updateStatus(Long id, Integer status, LocalDateTime approvedAt, Long approvedBy);

    @Modifying
    @Query("UPDATE Resource r SET r.downloadCount = r.downloadCount + 1 WHERE r.id = :id")
    int incrementDownloadCount(Long id);

    @Modifying
    @Query("UPDATE Resource r SET r.viewCount = r.viewCount + 1 WHERE r.id = :id")
    int incrementViewCount(Long id);

    @Query("SELECT COUNT(r) FROM Resource r WHERE r.status = :status")
    long countByStatus(Integer status);

    @Query("SELECT COUNT(r) FROM Resource r WHERE r.uploaderId = :uploaderId")
    long countByUploaderId(Long uploaderId);

    @Query("SELECT COUNT(r) FROM Resource r WHERE r.categoryId = :categoryId")
    long countByCategoryId(Long categoryId);

    @Query("SELECT COALESCE(SUM(r.downloadCount), 0) FROM Resource r")
    long getTotalDownloadCount();

    @Modifying
    @Query("UPDATE Resource r SET r.favoriteCount = :count WHERE r.id = :id")
    int updateFavoriteCount(Long id, long count);

    @Query("SELECT r FROM Resource r WHERE r.status = :status AND r.createdAt >= :startTime ORDER BY r.createdAt DESC")
    Page<Resource> findByStatusAndCreatedAtAfter(Integer status, LocalDateTime startTime, Pageable pageable);

    @Query("SELECT r FROM Resource r WHERE r.status = :status ORDER BY r.downloadCount DESC")
    Page<Resource> findByStatusOrderByDownloadCountDesc(Integer status, Pageable pageable);

    @Query("SELECT r FROM Resource r WHERE r.status = :status ORDER BY r.favoriteCount DESC")
    Page<Resource> findByStatusOrderByFavoriteCountDesc(Integer status, Pageable pageable);

    @Query("SELECT DISTINCT r FROM Resource r WHERE r.status = :status OR r.uploaderId = :uploaderId")
    Page<Resource> findByStatusOrUploaderId(Integer status, Long uploaderId, Pageable pageable);
}