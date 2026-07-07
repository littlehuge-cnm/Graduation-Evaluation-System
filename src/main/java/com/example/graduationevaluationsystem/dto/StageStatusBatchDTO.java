package com.example.graduationevaluationsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 批量启动环节 DTO
 */
@Data
public class StageStatusBatchDTO {

    @NotBlank(message = "环节不能为空")
    private String stage;

    @NotNull(message = "学生组号不能为空")
    private Integer studentGroupId;
}
