package com.example.graduationevaluationsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 师生关系
 */
@Data
@TableName("t_teacher_student")
public class TeacherStudent {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String studentNo;

    private String teacherNo;

    private String relationType;
}
