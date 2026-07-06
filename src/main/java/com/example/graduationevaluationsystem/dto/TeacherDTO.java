package com.example.graduationevaluationsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 教师新增/修改 DTO
 */
@Data
public class TeacherDTO {

    @NotBlank(message = "工号不能为空")
    private String teacherNo;

    @NotBlank(message = "姓名不能为空")
    private String teacherName;

    private String gender;

    private String department;

    private String title;

    private String phone;

    @NotBlank(message = "密码不能为空")
    private String password;

    private Integer accountStatus;
}
