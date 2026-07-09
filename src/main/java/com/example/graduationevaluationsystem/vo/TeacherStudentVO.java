package com.example.graduationevaluationsystem.vo;

import lombok.Data;

/**
 * 师生关系视图对象（含状态描述）
 */
@Data
public class TeacherStudentVO {

    private Integer id;
    private String studentNo;
    private String studentName;
    private String className;
    private String major;
    private String grade;
    private Integer studentGroupId;
    private String teacherNo;
    private String teacherName;
    private String relationType;
    private Integer relationStatus;
    private String relationStatusDesc;
}
