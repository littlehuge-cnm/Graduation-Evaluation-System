package com.example.graduationevaluationsystem.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 评价记录状态（t_score_record）
 */
@Getter
@AllArgsConstructor
public enum RecordStatus {

    DRAFT(1, "暂存"),
    CONFIRMED(2, "已确认");

    private final Integer code;
    private final String description;

    public static RecordStatus fromCode(Integer code) {
        for (RecordStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("无效的评价记录状态码: " + code);
    }
}
