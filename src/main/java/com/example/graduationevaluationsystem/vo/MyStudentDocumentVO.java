package com.example.graduationevaluationsystem.vo;

import lombok.Data;

/**
 * 指导教师待填写文档列表项 VO
 */
@Data
public class MyStudentDocumentVO {

    private String studentNo;
    private String studentName;
    private String className;
    private String major;

    /**
     * 文档记录编号（null 表示尚未创建文档）
     */
    private Integer docId;

    /**
     * 文档类型
     */
    private String docType;

    /**
     * 文档状态：1=草稿/2=已提交（null 表示未创建）
     */
    private Integer status;
    private String statusDesc;

    /**
     * 课题审批状态（仅任务书）
     */
    private Integer approvalStatus;
    private String approvalStatusDesc;
}
