package com.example.resourcesharingplatform.service;

import com.example.resourcesharingplatform.dto.resource.ResourceDTO;
import com.example.resourcesharingplatform.dto.resource.ResourceQueryRequest;
import com.example.resourcesharingplatform.dto.resource.ResourceUploadRequest;
import com.example.resourcesharingplatform.entity.Resource;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

/**
 * 资源服务接口
 */
public interface ResourceService {

    ResourceDTO uploadResource(Long userId, ResourceUploadRequest request, MultipartFile file);

    ResourceDTO getResourceById(Long id);

    void incrementViewCount(Long id);

    Page<ResourceDTO> listResources(ResourceQueryRequest request);

    void deleteResource(Long id);

    void approveResource(Long id, Long adminId);

    void rejectResource(Long id, Long adminId);

    byte[] downloadResource(Long id);

    ResourceDTO convertToDTO(Resource resource);
}