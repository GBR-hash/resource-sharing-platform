package com.example.resourcesharingplatform.service.impl;

import com.example.resourcesharingplatform.dto.resource.ResourceDTO;
import com.example.resourcesharingplatform.dto.resource.ResourceQueryRequest;
import com.example.resourcesharingplatform.dto.resource.ResourceUploadRequest;
import com.example.resourcesharingplatform.entity.Category;
import com.example.resourcesharingplatform.entity.CompetitionType;
import com.example.resourcesharingplatform.entity.Resource;
import com.example.resourcesharingplatform.entity.User;
import com.example.resourcesharingplatform.exception.BusinessException;
import com.example.resourcesharingplatform.repository.CategoryRepository;
import com.example.resourcesharingplatform.repository.CompetitionTypeRepository;
import com.example.resourcesharingplatform.repository.ResourceRepository;
import com.example.resourcesharingplatform.repository.UserRepository;
import com.example.resourcesharingplatform.service.ResourceService;
import com.example.resourcesharingplatform.util.Constants;
import com.example.resourcesharingplatform.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资源服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private final ResourceRepository resourceRepository;
    private final CategoryRepository categoryRepository;
    private final CompetitionTypeRepository competitionTypeRepository;
    private final UserRepository userRepository;
    private final FileUtil fileUtil;

    @Override
    @Transactional
    public ResourceDTO uploadResource(Long userId, ResourceUploadRequest request, MultipartFile file) {
        log.info("开始上传资源，用户ID: {}, 标题: {}, 分类ID: {}", userId, request.getTitle(), request.getCategoryId());
        
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }

        if (file.getSize() > Constants.MAX_FILE_SIZE) {
            throw new BusinessException("文件大小超过限制（最大50MB）");
        }

        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        log.info("文件信息：文件名={}, 类型={}, 大小={}", originalFilename, contentType, file.getSize());
        
        if (!fileUtil.isAllowedFileType(contentType) && !fileUtil.isAllowedFileExtension(originalFilename)) {
            throw new BusinessException("不支持的文件类型");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BusinessException("分类不存在"));
        log.info("分类信息：ID={}, 名称={}", category.getId(), category.getName());

        String filePath;
        try {
            filePath = fileUtil.saveFile(file);
            log.info("文件保存成功，路径：{}", filePath);
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new BusinessException("文件保存失败");
        }

        Resource resource = Resource.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .remark(request.getRemark())
                .fileName(file.getOriginalFilename())
                .filePath(filePath)
                .fileSize(file.getSize())
                .fileType(fileUtil.getFileCategory(file.getContentType()))
                .categoryId(request.getCategoryId())
                .competitionTypeId(request.getCompetitionTypeId())
                .uploaderId(userId)
                .status(Constants.RESOURCE_STATUS_PENDING)
                .downloadCount(0)
                .viewCount(0)
                .build();

        log.info("准备保存资源到数据库");
        Resource savedResource = resourceRepository.save(resource);
        log.info("资源保存成功，ID={}", savedResource.getId());
        
        return convertToDTO(savedResource);
    }

    @Override
    public ResourceDTO getResourceById(Long id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("资料不存在"));
        return convertToDTO(resource);
    }

    @Override
    @Transactional
    public void incrementViewCount(Long id) {
        resourceRepository.incrementViewCount(id);
    }

    @Override
    public Page<ResourceDTO> listResources(ResourceQueryRequest request) {
        Integer status = request.getStatus();
        Pageable pageable = PageRequest.of(
                request.getPageNumber(),
                request.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Resource> resources;

        // 处理下载量 Top 筛选
        if (request.getTopDownloads() != null && request.getTopDownloads() > 0) {
            Pageable topPageable = PageRequest.of(0, request.getTopDownloads(), Sort.by(Sort.Direction.DESC, "downloadCount"));
            resources = resourceRepository.findByStatusOrderByDownloadCountDesc(Constants.RESOURCE_STATUS_APPROVED, topPageable);
        }
        // 处理收藏量 Top 筛选
        else if (request.getTopFavorites() != null && request.getTopFavorites() > 0) {
            Pageable topPageable = PageRequest.of(0, request.getTopFavorites(), Sort.by(Sort.Direction.DESC, "favoriteCount"));
            resources = resourceRepository.findByStatusOrderByFavoriteCountDesc(Constants.RESOURCE_STATUS_APPROVED, topPageable);
        }
        // 处理时间范围筛选
        else if (request.getTimeRange() != null && !request.getTimeRange().equals("all")) {
            LocalDateTime startTime = getStartTimeByRange(request.getTimeRange());
            if (startTime != null) {
                resources = resourceRepository.findByStatusAndCreatedAtAfter(Constants.RESOURCE_STATUS_APPROVED, startTime, pageable);
            } else {
                resources = resourceRepository.findByStatus(Constants.RESOURCE_STATUS_APPROVED, pageable);
            }
        }
        // 处理自定义日期筛选
        else if (request.getCustomDate() != null && !request.getCustomDate().isEmpty()) {
            try {
                LocalDateTime startOfDay = LocalDate.parse(request.getCustomDate()).atStartOfDay();
                LocalDateTime endOfDay = startOfDay.plusDays(1);
                resources = resourceRepository.findByStatusAndTimeRange(Constants.RESOURCE_STATUS_APPROVED, startOfDay, endOfDay, pageable);
            } catch (Exception e) {
                resources = resourceRepository.findByStatus(Constants.RESOURCE_STATUS_APPROVED, pageable);
            }
        }
        // 原有筛选逻辑
        else if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
            resources = resourceRepository.findByTitleContainingOrDescriptionContaining(
                    request.getKeyword(), request.getKeyword(), pageable);
        } else if (request.getCategoryId() != null && request.getStatus() != null) {
            resources = resourceRepository.findByCategoryIdAndStatus(
                    request.getCategoryId(), request.getStatus(), pageable);
        } else if (request.getCompetitionTypeId() != null && request.getStatus() != null) {
            resources = resourceRepository.findByCompetitionTypeIdAndStatus(
                    request.getCompetitionTypeId(), request.getStatus(), pageable);
        } else if (request.getCategoryId() != null) {
            resources = resourceRepository.findByCategoryId(request.getCategoryId(), pageable);
        } else if (request.getCompetitionTypeId() != null) {
            resources = resourceRepository.findByCompetitionTypeId(request.getCompetitionTypeId(), pageable);
        } else if (request.getUploaderId() != null) {
            resources = resourceRepository.findByUploaderId(request.getUploaderId(), pageable);
        } else if (request.getStatus() != null) {
            resources = resourceRepository.findByStatus(request.getStatus(), pageable);
        } else if (request.getCurrentUserId() != null) {
            // 已登录用户：显示已发布的资料 + 自己上传的资料（包括审核中的）
            resources = resourceRepository.findByStatusOrUploaderId(Constants.RESOURCE_STATUS_APPROVED, request.getCurrentUserId(), pageable);
        } else {
            // 未登录用户：只显示已发布的资料
            resources = resourceRepository.findByStatus(Constants.RESOURCE_STATUS_APPROVED, pageable);
        }

        return resources.map(this::convertToDTO);
    }

    private LocalDateTime getStartTimeByRange(String timeRange) {
        LocalDateTime now = LocalDateTime.now();
        return switch (timeRange) {
            case "today" -> now.toLocalDate().atStartOfDay();
            case "yesterday" -> now.toLocalDate().minusDays(1).atStartOfDay();
            case "week" -> now.minusWeeks(1);
            case "month" -> now.minusMonths(1);
            case "threeMonths" -> now.minusMonths(3);
            case "year" -> now.minusYears(1);
            default -> null;
        };
    }

    @Override
    @Transactional
    public void deleteResource(Long id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("资料不存在"));

        fileUtil.deleteFile(resource.getFilePath());
        resourceRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void approveResource(Long id, Long adminId) {
        int affected = resourceRepository.updateStatus(
                id, Constants.RESOURCE_STATUS_APPROVED, LocalDateTime.now(), adminId);
        if (affected == 0) {
            throw new BusinessException("资料不存在");
        }
    }

    @Override
    @Transactional
    public void rejectResource(Long id, Long adminId) {
        int affected = resourceRepository.updateStatus(
                id, Constants.RESOURCE_STATUS_REJECTED, LocalDateTime.now(), adminId);
        if (affected == 0) {
            throw new BusinessException("资料不存在");
        }
    }

    @Override
    @Transactional
    public byte[] downloadResource(Long id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("资料不存在"));

        if (resource.getStatus() != Constants.RESOURCE_STATUS_APPROVED) {
            throw new BusinessException("资料未审核通过，无法下载");
        }

        resourceRepository.incrementDownloadCount(id);

        Path filePath = fileUtil.getFilePath(resource.getFilePath());
        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("文件读取失败", e);
            throw new BusinessException("文件读取失败");
        }
    }

    @Override
    public ResourceDTO convertToDTO(Resource resource) {
        ResourceDTO dto = ResourceDTO.from(resource);

        categoryRepository.findById(resource.getCategoryId())
                .ifPresent(category -> dto.setCategoryName(category.getName()));

        if (resource.getCompetitionTypeId() != null) {
            competitionTypeRepository.findById(resource.getCompetitionTypeId())
                    .ifPresent(type -> dto.setCompetitionTypeName(type.getName()));
        }

        userRepository.findById(resource.getUploaderId())
                .ifPresent(user -> dto.setUploaderName(user.getUsername()));

        return dto;
    }
}