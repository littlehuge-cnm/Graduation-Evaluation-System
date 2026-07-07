package com.example.graduationevaluationsystem.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 环节状态视图对象（含状态描述）
 */
@Data
public class StageStatusVO {

    private String stage;

    private Integer status;

    private String statusDesc;

    private LocalDateTime startTime;

    private LocalDateTime completeTime;
}
