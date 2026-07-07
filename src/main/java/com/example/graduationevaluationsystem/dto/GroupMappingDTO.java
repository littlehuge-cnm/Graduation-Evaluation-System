package com.example.graduationevaluationsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 环节对应关系新增/修改 DTO
 */
@Data
public class GroupMappingDTO {

    @NotBlank(message = "环节不能为空")
    private String stage;

    @NotNull(message = "教师组号不能为空")
    private Integer teacherGroupId;

    @NotNull(message = "学生组号不能为空")
    private Integer studentGroupId;
}
