package com.example.graduationevaluationsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 重置密码请求 DTO（超管操作）
 */
@Data
public class PasswordResetDTO {

    @NotBlank(message = "用户类型不能为空")
    private String userType;

    @NotBlank(message = "账号不能为空")
    private String username;

    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
