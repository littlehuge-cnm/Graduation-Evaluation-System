package com.example.graduationevaluationsystem.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 师生关系状态修改 DTO
 */
@Data
public class RelationStatusDTO {

    @NotNull(message = "关系状态不能为空")
    private Integer relationStatus;
}
