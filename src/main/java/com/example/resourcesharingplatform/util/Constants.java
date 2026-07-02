package com.example.resourcesharingplatform.util;

/**
 * 常量类
 */
public class Constants {

    private Constants() {
    }

    /**
     * 用户角色
     */
    public static final int ROLE_ADMIN = 1;
    public static final int ROLE_USER = 0;

    /**
     * 用户状态
     */
    public static final int USER_STATUS_ACTIVE = 1;
    public static final int USER_STATUS_INACTIVE = 0;

    /**
     * 资料状态
     */
    public static final int RESOURCE_STATUS_PENDING = 0;
    public static final int RESOURCE_STATUS_APPROVED = 1;
    public static final int RESOURCE_STATUS_REJECTED = 2;

    /**
     * JWT过期时间（毫秒）
     */
    public static final long JWT_EXPIRATION = 86400000L;

    /**
     * 文件大小限制（50MB）
     */
    public static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    /**
     * 允许的文件类型
     */
    public static final String[] ALLOWED_FILE_TYPES = {
            "image/jpeg", "image/png", "image/gif", "image/bmp",
            "application/pdf",
            "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "video/mp4", "video/mpeg", "video/quicktime", "video/x-msvideo",
            "text/plain", "application/json"
    };

    /**
     * 图片文件类型
     */
    public static final String[] IMAGE_TYPES = {"image/jpeg", "image/png", "image/gif", "image/bmp"};

    /**
     * 文档文件类型
     */
    public static final String[] DOCUMENT_TYPES = {
            "application/pdf",
            "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain", "application/json"
    };

    /**
     * 视频文件类型
     */
    public static final String[] VIDEO_TYPES = {"video/mp4", "video/mpeg", "video/quicktime", "video/x-msvideo"};
}