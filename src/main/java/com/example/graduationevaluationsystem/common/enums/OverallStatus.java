package com.example.graduationevaluationsystem.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 学生整体进度状态（t_student）
 */
/**
 * 待开始
 * 分组完成
 * 分配完成
 * 开题检查完成
 * 中期检查完成
 * 指导教师已评价
 * 评阅教师已评阅
 * 答辩完成
 * 委员会已评定
 */
@Getter
@AllArgsConstructor
public enum OverallStatus {

    UNASSIGNED(1, "待分配"),
    IN_PROGRESS(2, "进行中"),
    PENDING_DEFENSE(3, "待答辩"),
    COMPLETED(4, "已完成"),
    ABANDONED(5, "已弃做");

    private final Integer code;
    private final String description;

    public static OverallStatus fromCode(Integer code) {
        for (OverallStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("无效的学生整体进度状态码: " + code);
    }
}
