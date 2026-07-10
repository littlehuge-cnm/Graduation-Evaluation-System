package com.example.graduationevaluationsystem.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评价记录视图对象（含录入人姓名、状态描述）
 */
@Data
public class ScoreRecordVO {

    private Integer id;
    private String studentNo;
    private String itemType;
    private String subScores;
    private BigDecimal score;
    private String grade;
    private String comment;
    private String defenseRecord;
    private String recorderNo;
    private String recorderName;
    private LocalDateTime recordTime;
    private LocalDateTime updateTime;
    private Integer recordStatus;
    private String recordStatusDesc;
}
