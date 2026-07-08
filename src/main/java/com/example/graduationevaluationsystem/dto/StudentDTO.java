package com.example.graduationevaluationsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 学生新增/修改 DTO
 */
@Data
public class StudentDTO {

    @NotBlank(message = "学号不能为空")
    private String studentNo;

    @NotBlank(message = "姓名不能为空")
    private String studentName;

    private String gender;

    private String className;

    private String major;

    private String grade;

    private Integer studentGroupId;

    @NotBlank(message = "密码不能为空")
    private String password;

    private Integer accountStatus;

    private Integer overallStatus;
}
