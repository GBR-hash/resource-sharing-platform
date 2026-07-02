package com.example.resourcesharingplatform.dto.resource;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 资料上传请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceUploadRequest {

    @NotBlank(message = "标题不能为空")
    private String title;

    private String description;

    private String remark;

    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    private Long competitionTypeId;
}