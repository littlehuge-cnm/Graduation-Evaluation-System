package com.example.graduationevaluationsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 文档保存/修改 DTO
 */
@Data
public class DocumentDTO {

    @NotBlank(message = "学号不能为空")
    private String studentNo;

    @NotBlank(message = "文档类型不能为空")
    private String docType;

    private String title;

    private String subjectCategory;

    private String subjectType;

    private String subjectNewOld;

    private String content;

    @NotNull(message = "文档状态不能为空")
    private Integer status;
}
