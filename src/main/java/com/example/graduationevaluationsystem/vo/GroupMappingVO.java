package com.example.graduationevaluationsystem.vo;

import lombok.Data;

/**
 * 环节对应关系视图对象（含教师组名、学生组名）
 */
@Data
public class GroupMappingVO {

    private Integer id;
    private String stage;
    private Integer teacherGroupId;
    private String teacherGroupName;
    private Integer studentGroupId;
    private String studentGroupName;
}
