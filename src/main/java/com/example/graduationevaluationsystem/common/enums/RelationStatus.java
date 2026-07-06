package com.example.graduationevaluationsystem.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 师生关系状态（t_teacher_student）
 */
@Getter
@AllArgsConstructor
public enum RelationStatus {

    ACTIVE(1, "生效"),
    TERMINATED(2, "已解除");

    private final Integer code;
    private final String description;

    public static RelationStatus fromCode(Integer code) {
        for (RelationStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("无效的师生关系状态码: " + code);
    }
}
