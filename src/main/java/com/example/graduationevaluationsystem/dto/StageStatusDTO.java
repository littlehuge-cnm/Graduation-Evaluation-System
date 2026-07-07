package com.example.graduationevaluationsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 环节状态启动/完成 DTO
 */
@Data
public class StageStatusDTO {

    @NotBlank(message = "学号不能为空")
    private String studentNo;

    @NotBlank(message = "环节不能为空")
    private String stage;
}
