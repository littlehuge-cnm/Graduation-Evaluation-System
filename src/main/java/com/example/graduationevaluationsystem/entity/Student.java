package com.example.graduationevaluationsystem.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 学生
 */
@Data
@TableName("t_student")
public class Student {

    @TableId
    private String studentNo;

    private String studentName;

    private String gender;

    private String className;

    private String major;

    private String grade;

    private Integer studentGroupId;

    private String password;
}
