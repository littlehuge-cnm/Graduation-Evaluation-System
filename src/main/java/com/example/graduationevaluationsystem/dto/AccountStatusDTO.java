package com.example.graduationevaluationsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 账号状态修改 DTO（超管操作）
 */
@Data
public class AccountStatusDTO {

    @NotBlank(message = "用户类型不能为空")
    private String userType;

    @NotBlank(message = "账号不能为空")
    private String username;

    @NotNull(message = "账号状态不能为空")
    private Integer accountStatus;
}
