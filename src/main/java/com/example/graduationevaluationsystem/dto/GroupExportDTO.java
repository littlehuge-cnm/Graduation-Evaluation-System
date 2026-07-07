package com.example.graduationevaluationsystem.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 按学生组批量导出 DTO
 */
@Data
public class GroupExportDTO {

    @NotNull(message = "学生组号不能为空")
    private Integer studentGroupId;
}
