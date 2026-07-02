package com.example.resourcesharingplatform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 资料实体类
 */
@Entity
@Table(name = "resources")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(length = 500)
    private String remark;

    @Column(name = "file_name", nullable = false, length = 200)
    private String fileName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "file_type", length = 50)
    private String fileType;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "competition_type_id")
    private Long competitionTypeId;

    @Column(name = "uploader_id", nullable = false)
    private Long uploaderId;

    @Column(nullable = false)
    @Builder.Default
    private Integer status = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer downloadCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer favoriteCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by")
    private Long approvedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}