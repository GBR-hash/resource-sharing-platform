package com.example.resourcesharingplatform.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户更新请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {

    @Email(message = "邮箱格式不正确")
    private String email;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号码格式不正确")
    private String phone;

    private String realName;
}