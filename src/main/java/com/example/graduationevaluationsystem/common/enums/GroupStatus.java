package com.example.graduationevaluationsystem.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 教师分组状态（t_teacher_group）
 */
@Getter
@AllArgsConstructor
public enum GroupStatus {

    PENDING(1, "待启用"),
    ACTIVE(2, "已启用"),
    INACTIVE(3, "已停用");

    private final Integer code;
    private final String description;

    public static GroupStatus fromCode(Integer code) {
        for (GroupStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("无效的教师分组状态码: " + code);
    }
}
