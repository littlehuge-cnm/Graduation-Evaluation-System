package com.example.graduationevaluationsystem.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档视图对象（含状态描述、学生姓名）
 */
@Data
public class DocumentVO {

    private Integer id;
    private String studentNo;
    private String studentName;
    private String docType;
    private String title;
    private String subjectCategory;
    private String subjectType;
    private String subjectNewOld;
    private String content;
    private Integer status;
    private String statusDesc;
    private Integer approvalStatus;
    private String approvalStatusDesc;
    private LocalDateTime submitTime;
    private LocalDateTime updateTime;
}
