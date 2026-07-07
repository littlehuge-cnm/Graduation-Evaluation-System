package com.example.graduationevaluationsystem.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 学生整体进度状态修改 DTO（超管操作）
 */
@Data
public class OverallStatusDTO {

    @NotNull(message = "整体进度状态不能为空")
    private Integer overallStatus;
}
