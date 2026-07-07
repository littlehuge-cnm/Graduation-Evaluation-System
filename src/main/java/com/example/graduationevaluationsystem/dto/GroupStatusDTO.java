package com.example.graduationevaluationsystem.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 教师分组状态修改 DTO
 */
@Data
public class GroupStatusDTO {

    @NotNull(message = "分组状态不能为空")
    private Integer groupStatus;
}
