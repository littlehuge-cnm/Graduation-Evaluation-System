package com.example.graduationevaluationsystem.vo;

import lombok.Data;

/**
 * 学生整体进度状态 VO
 */
@Data
public class StudentOverallStatusVO {

    private String studentNo;
    private Integer overallStatus;
    private String overallStatusDesc;
}
