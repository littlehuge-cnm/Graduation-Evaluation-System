package com.example.graduationevaluationsystem.vo;

import lombok.Data;

import java.util.List;

/**
 * 学生全部状态 VO（一次返回整体进度、各环节状态、文档状态、评价记录状态）
 */
@Data
public class StudentAllStatusVO {

    private String studentNo;
    private Integer overallStatus;
    private String overallStatusDesc;

    private List<StageStatusItem> stageStatus;
    private List<DocumentStatusItem> documentStatus;
    private List<ScoreRecordStatusItem> scoreRecordStatus;

    @Data
    public static class StageStatusItem {
        private String stage;
        private Integer status;
        private String statusDesc;
    }

    @Data
    public static class DocumentStatusItem {
        private String docType;
        private Integer status;
        private String statusDesc;
        private Integer approvalStatus;
        private String approvalStatusDesc;
    }

    @Data
    public static class ScoreRecordStatusItem {
        private String itemType;
        private Integer recordStatus;
        private String recordStatusDesc;
    }
}
