package com.example.graduationevaluationsystem.vo;

import lombok.Data;

/**
 * 学生视图对象（不含密码）
 */
@Data
public class StudentVO {

    private String studentNo;
    private String studentName;
    private String gender;
    private String className;
    private String major;
    private String grade;
    private Integer studentGroupId;
    private Integer accountStatus;
    private String accountStatusDesc;
    private Integer overallStatus;
    private String overallStatusDesc;
}
