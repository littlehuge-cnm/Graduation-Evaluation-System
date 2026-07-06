package com.example.graduationevaluationsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 环节对应关系
 */
@Data
@TableName("t_group_mapping")
public class GroupMapping {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String stage;

    private Integer teacherGroupId;

    private Integer studentGroupId;
}
