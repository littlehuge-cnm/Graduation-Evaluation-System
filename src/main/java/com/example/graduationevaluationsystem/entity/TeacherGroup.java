package com.example.graduationevaluationsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 教师分组
 */
@Data
@TableName("t_teacher_group")
public class TeacherGroup {

    @TableId(type = IdType.AUTO)
    private Integer groupId;

    private String groupName;

    private String leaderNo;

    private String secretaryNo;

    private String memberNo;
}
