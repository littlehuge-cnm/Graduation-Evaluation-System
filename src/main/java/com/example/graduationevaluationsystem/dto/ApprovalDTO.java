package com.example.graduationevaluationsystem.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 课题审批 DTO（系审/院审共用）
 */
@Data
public class ApprovalDTO {

    @NotNull(message = "审批状态不能为空")
    private Integer approvalStatus;

    private String comment;
}
