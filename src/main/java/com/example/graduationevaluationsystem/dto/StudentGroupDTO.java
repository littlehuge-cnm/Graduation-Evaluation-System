package com.example.graduationevaluationsystem.dto;

import lombok.Data;

import java.util.List;

/**
 * 学生分组新增/修改 DTO
 */
@Data
public class StudentGroupDTO {

    private String groupName;

    /**
     * 组内学号列表（创建/修改时传入，后端据此更新学生的 student_group_id）
     */
    private List<String> studentNos;
}
