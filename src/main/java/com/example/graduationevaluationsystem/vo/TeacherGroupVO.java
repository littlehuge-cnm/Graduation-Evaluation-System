package com.example.graduationevaluationsystem.vo;

import lombok.Data;

/**
 * 教师分组视图对象（含教师姓名与状态描述）
 */
@Data
public class TeacherGroupVO {

    private Integer groupId;
    private String groupName;
    private String leaderNo;
    private String leaderName;
    private String secretaryNo;
    private String secretaryName;
    private String memberNo;
    private String memberName;
    private Integer groupStatus;
    private String groupStatusDesc;
}
