package com.example.graduationevaluationsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评价记录
 */
@Data
@TableName("t_score_record")
public class ScoreRecord {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String studentNo;

    private String itemType;

    private String subScores;

    private BigDecimal score;

    private String grade;

    private String comment;

    private String recorderNo;

    private LocalDateTime recordTime;

    private LocalDateTime updateTime;
}
