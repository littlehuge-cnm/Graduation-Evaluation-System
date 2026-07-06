package com.example.graduationevaluationsystem.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 教师
 */
@Data
@TableName("t_teacher")
public class Teacher {

    @TableId
    private String teacherNo;

    private String teacherName;

    private String gender;

    private String department;

    private String title;

    private String phone;

    private String password;

    private Integer accountStatus;
}
