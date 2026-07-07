package com.example.graduationevaluationsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 师生关系新增/修改 DTO
 */
@Data
public class TeacherStudentDTO {

    @NotBlank(message = "学号不能为空")
    private String studentNo;

    @NotBlank(message = "教师工号不能为空")
    private String teacherNo;

    @NotBlank(message = "关系类型不能为空")
    private String relationType;

    private Integer relationStatus;
}
