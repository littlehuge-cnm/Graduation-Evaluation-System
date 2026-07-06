package com.example.graduationevaluationsystem.vo;

import lombok.Data;

/**
 * 教师视图对象（不含密码）
 */
@Data
public class TeacherVO {

    private String teacherNo;
    private String teacherName;
    private String gender;
    private String department;
    private String title;
    private String phone;
    private Integer accountStatus;
    private String accountStatusDesc;
}
