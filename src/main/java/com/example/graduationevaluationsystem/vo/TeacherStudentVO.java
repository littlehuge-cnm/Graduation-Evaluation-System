package com.example.graduationevaluationsystem.vo;

import lombok.Data;

/**
 * 教师关联的学生视图对象
 */
@Data
public class TeacherStudentVO {

    private String studentNo;
    private String studentName;
    private String gender;
    private String className;
    private String major;
    private String grade;
    private Integer studentGroupId;
    private String relationType;
}
