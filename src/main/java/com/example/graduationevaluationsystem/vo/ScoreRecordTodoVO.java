package com.example.graduationevaluationsystem.vo;

import lombok.Data;

/**
 * 待录入评价记录列表项 VO
 */
@Data
public class ScoreRecordTodoVO {

    private String studentNo;
    private String studentName;
    private String className;

    /**
     * 条目类型（开题成绩/外文翻译/中期检查/指导评语/评阅评语/答辩记录/答辩成绩/委员会评定）
     */
    private String itemType;

    /**
     * 录入角色（组长/指导教师/评阅教师/秘书/超级管理员）
     */
    private String recorderRole;

    /**
     * 已有记录编号（null 表示尚未录入）
     */
    private Integer recordId;

    /**
     * 已有记录状态（null 表示尚未录入）
     */
    private Integer recordStatus;
    private String recordStatusDesc;
}
