package com.example.graduationevaluationsystem.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 环节状态（t_stage_status）
 */
@Getter
@AllArgsConstructor
public enum StageStatusType {

    NOT_STARTED(1, "未开始"),
    IN_PROGRESS(2, "进行中"),
    COMPLETED(3, "已完成");

    private final Integer code;
    private final String description;

    public static StageStatusType fromCode(Integer code) {
        for (StageStatusType status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("无效的环节状态码: " + code);
    }
}
