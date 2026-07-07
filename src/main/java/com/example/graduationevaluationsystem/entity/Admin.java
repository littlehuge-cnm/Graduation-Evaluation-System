package com.example.graduationevaluationsystem.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 超级管理员
 */
@Data
@TableName("t_admin")
public class Admin {

    @TableId
    private String adminId;

    private String adminName;

    private String password;

    private Integer accountStatus;
}
