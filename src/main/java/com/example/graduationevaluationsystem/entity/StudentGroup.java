package com.example.graduationevaluationsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 学生分组
 */
@Data
@TableName("t_student_group")
public class StudentGroup {

    @TableId(type = IdType.AUTO)
    private Integer groupId;

    private String groupName;
}
