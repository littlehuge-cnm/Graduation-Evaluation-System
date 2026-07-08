package com.example.graduationevaluationsystem.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 批量分配环节对应关系 DTO
 */
@Data
public class GroupMappingBatchAssignDTO {

    @NotNull(message = "学生组不能为空")
    private Integer studentGroupId;

    private Integer openingTeacherGroupId;

    private Integer midtermTeacherGroupId;

    private Integer defenseTeacherGroupId;
}
