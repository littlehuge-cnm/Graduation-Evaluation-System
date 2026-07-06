package com.example.graduationevaluationsystem.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 课题审批状态（t_document，仅任务书使用）
 */
@Getter
@AllArgsConstructor
public enum ApprovalStatus {

    DEPT_PENDING(1, "待系审"),
    DEPT_APPROVED(2, "系通过"),
    DEPT_REJECTED(3, "系驳回"),
    COLLEGE_PENDING(4, "待院审"),
    COLLEGE_APPROVED(5, "院通过"),
    COLLEGE_REJECTED(6, "院驳回");

    private final Integer code;
    private final String description;

    public static ApprovalStatus fromCode(Integer code) {
        for (ApprovalStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("无效的课题审批状态码: " + code);
    }
}
