package com.example.graduationevaluationsystem.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文档状态（t_document）
 */
@Getter
@AllArgsConstructor
public enum DocumentStatus {

    DRAFT(1, "草稿"),
    SUBMITTED(2, "已提交");

    private final Integer code;
    private final String description;

    public static DocumentStatus fromCode(Integer code) {
        for (DocumentStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("无效的文档状态码: " + code);
    }
}
