package com.example.resourcesharingplatform.dto.resource;

import com.example.resourcesharingplatform.entity.Resource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 资料响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceDTO {

    private Long id;
    private String title;
    private String description;
    private String remark;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String fileType;
    private Long categoryId;
    private String categoryName;
    private Long competitionTypeId;
    private String competitionTypeName;
    private Long uploaderId;
    private String uploaderName;
    private Integer status;
    private Integer downloadCount;
    private Integer viewCount;
    private Integer favoriteCount;
    private Boolean isFavorite;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime approvedAt;

    public static ResourceDTO from(Resource resource) {
        return ResourceDTO.builder()
                .id(resource.getId())
                .title(resource.getTitle())
                .description(resource.getDescription())
                .remark(resource.getRemark())
                .fileName(resource.getFileName())
                .filePath(resource.getFilePath())
                .fileSize(resource.getFileSize())
                .fileType(resource.getFileType())
                .categoryId(resource.getCategoryId())
                .competitionTypeId(resource.getCompetitionTypeId())
                .uploaderId(resource.getUploaderId())
                .status(resource.getStatus())
                .downloadCount(resource.getDownloadCount())
                .viewCount(resource.getViewCount())
                .favoriteCount(resource.getFavoriteCount())
                .createdAt(resource.getCreatedAt())
                .updatedAt(resource.getUpdatedAt())
                .approvedAt(resource.getApprovedAt())
                .build();
    }
}