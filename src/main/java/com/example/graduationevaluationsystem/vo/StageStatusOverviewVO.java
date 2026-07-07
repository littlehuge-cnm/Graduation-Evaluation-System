package com.example.graduationevaluationsystem.vo;

import lombok.Data;

/**
 * 环节进度总览视图对象
 */
@Data
public class StageStatusOverviewVO {

    private String stage;

    private Integer studentGroupId;

    /**
     * 状态统计：key=状态描述（未开始/进行中/已完成），value=人数
     */
    private java.util.Map<String, Integer> statistics;

    /**
     * 学生环节状态明细列表
     */
    private java.util.List<StudentStageStatusItem> list;

    /**
     * 单个学生的环节状态明细
     */
    @Data
    public static class StudentStageStatusItem {
        private String studentNo;
        private String studentName;
        private Integer status;
        private String statusDesc;
    }
}
