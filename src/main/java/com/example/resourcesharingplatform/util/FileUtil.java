package com.example.resourcesharingplatform.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 文件工具类
 */
@Slf4j
@Component
public class FileUtil {

    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * 保存文件
     */
    public String saveFile(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        log.info("开始保存文件：{}", originalFilename);
        log.info("上传目录配置：{}", uploadDir);
        
        String extension = getFileExtension(originalFilename);
        String newFilename = UUID.randomUUID().toString() + extension;

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path directory = Paths.get(uploadDir, datePath).toAbsolutePath().normalize();

        log.info("目标目录：{}", directory);
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
            log.info("创建目录成功：{}", directory);
        }

        Path filePath = directory.resolve(newFilename);
        log.info("文件完整路径：{}", filePath);
        
        file.transferTo(filePath.toFile());
        log.info("文件保存成功：{}", filePath);

        return datePath + "/" + newFilename;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * 获取文件路径
     */
    public Path getFilePath(String relativePath) {
        return Paths.get(uploadDir, relativePath);
    }

    /**
     * 删除文件
     */
    public boolean deleteFile(String relativePath) {
        try {
            Path filePath = getFilePath(relativePath);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 检查文件类型是否允许
     */
    public boolean isAllowedFileType(String contentType) {
        if (contentType == null) {
            return false;
        }
        for (String allowedType : Constants.ALLOWED_FILE_TYPES) {
            if (allowedType.equalsIgnoreCase(contentType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查文件扩展名是否允许
     */
    public boolean isAllowedFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return false;
        }
        String extension = getFileExtension(filename).toLowerCase();
        String[] allowedExtensions = {
            ".jpg", ".jpeg", ".png", ".gif", ".bmp",
            ".pdf",
            ".doc", ".docx",
            ".xls", ".xlsx",
            ".ppt", ".pptx",
            ".mp4", ".mpeg", ".mov", ".avi",
            ".txt", ".json"
        };
        for (String ext : allowedExtensions) {
            if (ext.equals(extension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取文件类型分类
     */
    public String getFileCategory(String contentType) {
        if (contentType == null) {
            return "other";
        }
        for (String type : Constants.IMAGE_TYPES) {
            if (type.equalsIgnoreCase(contentType)) {
                return "image";
            }
        }
        for (String type : Constants.DOCUMENT_TYPES) {
            if (type.equalsIgnoreCase(contentType)) {
                return "document";
            }
        }
        for (String type : Constants.VIDEO_TYPES) {
            if (type.equalsIgnoreCase(contentType)) {
                return "video";
            }
        }
        return "other";
    }
}