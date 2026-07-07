package com.example.graduationevaluationsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 评价记录录入/修改 DTO
 */
@Data
public class ScoreRecordDTO {

    @NotBlank(message = "学号不能为空")
    private String studentNo;

    @NotBlank(message = "条目类型不能为空")
    private String itemType;

    /**
     * 分项成绩，逗号分隔（如 "4,3,4"）
     */
    private String subScores;

    /**
     * 总成绩（若未传则后端按分项求和）
     */
    private BigDecimal score;

    /**
     * 等级（仅委员会评定填写）
     */
    private String grade;

    /**
     * 评语/记录内容
     */
    private String comment;

    /**
     * 记录状态：1=暂存（默认）/2=已确认
     */
    private Integer recordStatus;
}
