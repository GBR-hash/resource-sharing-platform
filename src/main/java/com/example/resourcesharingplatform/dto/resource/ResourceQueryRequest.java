package com.example.resourcesharingplatform.dto.resource;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 资料查询请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceQueryRequest {

    private String keyword;
    private Long categoryId;
    private Long competitionTypeId;
    private Integer status;
    private Long uploaderId;
    private String timeRange; // 时间范围：today, yesterday, week, month, threeMonths, year, all
    private String customDate; // 自定义日期：yyyy-MM-dd
    private Integer topDownloads; // 下载量 Top N
    private Integer topFavorites; // 收藏量 Top N
    private Long currentUserId; // 当前登录用户 ID
    private Integer pageNumber = 0;
    private Integer pageSize = 10;
}