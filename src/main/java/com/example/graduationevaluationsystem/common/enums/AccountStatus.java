package com.example.graduationevaluationsystem.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 账号状态（t_admin / t_teacher / t_student）
 */
@Getter
@AllArgsConstructor
public enum AccountStatus {

    ENABLED(1, "启用"),
    DISABLED(2, "禁用");

    private final Integer code;
    private final String description;

    public static AccountStatus fromCode(Integer code) {
        for (AccountStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("无效的账号状态码: " + code);
    }
}
