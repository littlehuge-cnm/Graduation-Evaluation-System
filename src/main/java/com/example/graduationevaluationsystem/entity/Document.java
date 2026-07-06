package com.example.graduationevaluationsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档（任务书/指导书）
 */
@Data
@TableName("t_document")
public class Document {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String studentNo;

    private String docType;

    private String title;

    private String subjectCategory;

    private String subjectType;

    private String subjectNewOld;

    private String content;

    private String status;

    private LocalDateTime submitTime;

    private LocalDateTime updateTime;
}
