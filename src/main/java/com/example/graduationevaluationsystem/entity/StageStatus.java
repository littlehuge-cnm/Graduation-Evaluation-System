package com.example.graduationevaluationsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 环节状态
 */
@Data
@TableName("t_stage_status")
public class StageStatus {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String studentNo;

    private String stage;

    private Integer status;

    private LocalDateTime startTime;

    private LocalDateTime completeTime;
}
