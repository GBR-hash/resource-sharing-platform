package com.example.resourcesharingplatform.controller;

import com.example.resourcesharingplatform.dto.PageResponse;
import com.example.resourcesharingplatform.dto.Result;
import com.example.resourcesharingplatform.dto.resource.ResourceDTO;
import com.example.resourcesharingplatform.dto.resource.ResourceQueryRequest;
import com.example.resourcesharingplatform.dto.resource.ResourceUploadRequest;
import com.example.resourcesharingplatform.entity.Resource;
import com.example.resourcesharingplatform.entity.User;
import com.example.resourcesharingplatform.repository.FavoriteRepository;
import com.example.resourcesharingplatform.repository.ResourceRepository;
import com.example.resourcesharingplatform.repository.UserRepository;
import com.example.resourcesharingplatform.service.ResourceService;
import com.example.resourcesharingplatform.util.Constants;
import com.example.resourcesharingplatform.util.FileUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * 资源控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final FavoriteRepository favoriteRepository;
    private final FileUtil fileUtil;

    @PostMapping("/upload")
    public Result<ResourceDTO> uploadResource(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "remark", required = false) String remark,
            @RequestParam(value = "competitionTypeId", required = false) Long competitionTypeId) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        log.info("当前登录用户：{}, ID: {}", currentUser.getUsername(), currentUser.getId());

        ResourceUploadRequest request = new ResourceUploadRequest();
        request.setTitle(title);
        request.setDescription(description);
        request.setRemark(remark);
        request.setCategoryId(categoryId);
        request.setCompetitionTypeId(competitionTypeId);

        ResourceDTO resource = resourceService.uploadResource(currentUser.getId(), request, file);
        return Result.success("上传成功，等待审核", resource);
    }

    @GetMapping("/{id:[0-9]+}")
    public Result<ResourceDTO> getResource(@PathVariable Long id) {
        ResourceDTO resource = resourceService.getResourceById(id);
        resourceService.incrementViewCount(id);
        return Result.success(resource);
    }

    @GetMapping
    public Result<PageResponse<ResourceDTO>> listResources(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "competitionTypeId", required = false) Long competitionTypeId,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "timeRange", required = false) String timeRange,
            @RequestParam(value = "customDate", required = false) String customDate,
            @RequestParam(value = "topDownloads", required = false) Integer topDownloads,
            @RequestParam(value = "topFavorites", required = false) Integer topFavorites,
            @RequestParam(value = "pageNumber", defaultValue = "0") Integer pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {

        ResourceQueryRequest request = new ResourceQueryRequest();
        request.setKeyword(keyword);
        request.setCategoryId(categoryId);
        request.setCompetitionTypeId(competitionTypeId);
        request.setStatus(status);
        request.setTimeRange(timeRange);
        request.setCustomDate(customDate);
        request.setTopDownloads(topDownloads);
        request.setTopFavorites(topFavorites);
        request.setPageNumber(pageNumber);
        request.setPageSize(pageSize);

        // 获取当前登录用户 ID
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            User currentUser = (User) authentication.getPrincipal();
            request.setCurrentUserId(currentUser.getId());
        }

        Page<ResourceDTO> resources = resourceService.listResources(request);

        // 设置收藏状态
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            User currentUser = (User) authentication.getPrincipal();
            List<Long> favoriteIds = favoriteRepository.findResourceIdsByUserId(currentUser.getId());
            resources.getContent().forEach(dto -> {
                dto.setIsFavorite(favoriteIds.contains(dto.getId()));
            });
        } else {
            resources.getContent().forEach(dto -> dto.setIsFavorite(false));
        }

        return Result.success(PageResponse.from(resources));
    }

    @GetMapping("/download/{id:[0-9]+}")
    public ResponseEntity<byte[]> downloadResource(@PathVariable Long id) {
        byte[] fileContent = resourceService.downloadResource(id);
        Resource resource = resourceRepository.findById(id).orElse(null);

        if (resource == null) {
            return ResponseEntity.notFound().build();
        }

        String fileName = URLEncoder.encode(resource.getFileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        // 根据文件类型设置 Content-Type，图片和视频支持 inline 预览
        MediaType mediaType = getMediaType(resource.getFileType());
        boolean isPreviewable = mediaType != null && ("image".equals(mediaType.getType()) || "video".equals(mediaType.getType()));
        String disposition = isPreviewable
                ? "inline; filename=\"" + fileName + "\""
                : "attachment; filename=\"" + fileName + "\"";

        return ResponseEntity.ok()
                .contentType(mediaType != null ? mediaType : MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(fileContent);
    }

    @GetMapping("/preview/{id:[0-9]+}")
    public ResponseEntity<byte[]> previewResource(@PathVariable Long id) {
        Resource resource = resourceRepository.findById(id).orElse(null);

        if (resource == null) {
            return ResponseEntity.notFound().build();
        }

        // 预览不检查状态，也不增加下载计数
        Path filePath = fileUtil.getFilePath(resource.getFilePath());
        try {
            byte[] fileContent = Files.readAllBytes(filePath);
            MediaType mediaType = getMediaType(resource.getFileType());

            return ResponseEntity.ok()
                    .contentType(mediaType != null ? mediaType : MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + 
                            URLEncoder.encode(resource.getFileName(), StandardCharsets.UTF_8).replace("+", "%20") + "\"")
                    .body(fileContent);
        } catch (IOException e) {
            log.error("文件读取失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private MediaType getMediaType(String fileType) {
        if (fileType == null) return null;
        return switch (fileType.toLowerCase()) {
            case "image" -> MediaType.IMAGE_JPEG;
            case "video" -> MediaType.valueOf("video/mp4");
            case "document" -> MediaType.APPLICATION_PDF;
            default -> null;
        };
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteResource(@PathVariable Long id) {
        resourceService.deleteResource(id);
        return Result.success("删除成功");
    }
}