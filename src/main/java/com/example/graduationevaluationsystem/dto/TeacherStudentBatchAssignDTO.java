package com.example.graduationevaluationsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 批量分配指导/评阅教师 DTO
 */
@Data
public class TeacherStudentBatchAssignDTO {

    @NotBlank(message = "学号不能为空")
    private String studentNo;

    private String guideTeacherNo;

    private String reviewTeacherNo;
}
