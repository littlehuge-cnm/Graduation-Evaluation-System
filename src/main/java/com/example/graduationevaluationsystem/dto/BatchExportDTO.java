package com.example.graduationevaluationsystem.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量导出评价手册 DTO
 */
@Data
public class BatchExportDTO {

    @NotEmpty(message = "学号列表不能为空")
    private List<String> studentNos;
}
