package com.example.graduationevaluationsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 教师分组新增/修改 DTO
 */
@Data
public class TeacherGroupDTO {

    private String groupName;

    @NotBlank(message = "组长工号不能为空")
    private String leaderNo;

    @NotBlank(message = "秘书工号不能为空")
    private String secretaryNo;

    @NotBlank(message = "普通成员工号不能为空")
    private String memberNo;

    private Integer groupStatus;
}
