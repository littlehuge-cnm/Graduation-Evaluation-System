package com.example.graduationevaluationsystem.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 评价记录条目类型（对应接口文档 7.9 条目类型表）
 * <p>
 * 每个枚举值封装了：录入角色、分项满分配置、总满分。
 * 用于录入时校验 subScores 的分项数量和各项上限。
 */
@Getter
@AllArgsConstructor
public enum ItemType {

    OPENING_SCORE("开题报告成绩", "组长", new int[] { 4, 4, 4 }, 12),
    TRANSLATION("外文翻译", "指导教师", new int[] { 1, 1, 1 }, 3),
    MIDTERM_CHECK("中期检查成绩", "组长", new int[] { 5, 5, 5 }, 15),
    SUPERVISION_COMMENT("指导评语", "指导教师", new int[] { 3, 3, 3, 3, 3 }, 15),
    REVIEW_COMMENT("评阅评语", "评阅教师", new int[] { 4, 4, 4, 3 }, 15),
    DEFENSE_RECORD("答辩记录", "秘书", null, null),
    DEFENSE_SCORE("毕业答辩成绩", "组长", new int[] { 10, 10, 10, 10 }, 40),
    COMMITTEE_EVALUATION("委员会评定", "超级管理员", null, 100);

    /**
     * 条目名称
     */
    private final String name;

    /**
     * 录入角色
     */
    private final String recorderRole;

    /**
     * 分项满分配置（null 表示无分项，如答辩记录/委员会评定）
     */
    private final int[] subScoreMaxes;

    /**
     * 总满分（null 表示无总分限制，如答辩记录）
     */
    private final Integer totalMax;

    /**
     * 按名称获取枚举
     */
    public static ItemType fromName(String name) {
        for (ItemType type : values()) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("无效的条目类型: " + name);
    }

    /**
     * 是否有分项成绩
     */
    public boolean hasSubScores() {
        return subScoreMaxes != null;
    }

    /**
     * 校验分项成绩字符串是否合法
     * <p>
     * 校验规则：
     * 1. 有分项的类型：分项数量必须匹配，每项不超过对应满分
     * 2. 无分项的类型：subScores 必须为空
     *
     * @param subScores 分项成绩字符串（逗号分隔，如 "4,3,4"）
     * @throws IllegalArgumentException 校验不通过时抛出
     */
    public void validateSubScores(String subScores) {
        if (!hasSubScores()) {
            if (subScores != null && !subScores.isBlank()) {
                throw new RuntimeException(name + "不需要填写分项成绩");
            }
            return;
        }

        if (subScores == null || subScores.isBlank()) {
            throw new RuntimeException(name + "需要填写分项成绩");
        }

        String[] parts = subScores.split(",");
        if (parts.length != subScoreMaxes.length) {
            throw new RuntimeException(name + "的分项成绩应为" + subScoreMaxes.length + "项，实际为" + parts.length + "项");
        }

        for (int i = 0; i < parts.length; i++) {
            int value;
            try {
                value = Integer.parseInt(parts[i].trim());
            } catch (NumberFormatException e) {
                throw new RuntimeException(name + "的第" + (i + 1) + "项成绩不是有效数字: " + parts[i]);
            }
            if (value < 0 || value > subScoreMaxes[i]) {
                throw new RuntimeException(name + "的第" + (i + 1) + "项成绩应在0~" + subScoreMaxes[i] + "之间，实际为" + value);
            }
        }
    }

    /**
     * 从分项成绩计算总成绩
     *
     * @param subScores 分项成绩字符串（逗号分隔）
     * @return 总成绩（分项求和）
     */
    public int calculateScore(String subScores) {
        if (!hasSubScores() || subScores == null || subScores.isBlank()) {
            return 0;
        }
        return Arrays.stream(subScores.split(","))
                .mapToInt(s -> Integer.parseInt(s.trim()))
                .sum();
    }

    /**
     * 获取该条目对应的环节名称
     */
    public String getStage() {
        return switch (this) {
            case OPENING_SCORE, TRANSLATION -> "开题";
            case MIDTERM_CHECK, SUPERVISION_COMMENT, REVIEW_COMMENT -> "中期";
            case DEFENSE_RECORD, DEFENSE_SCORE, COMMITTEE_EVALUATION -> "答辩";
        };
    }

    /**
     * 获取录入该条目所需的环节状态：
     * - 2 = 进行中（成绩类条目录入时环节须处于进行中）
     * - 3 = 已完成（委员会评定须答辩环节已完成）
     * - null = 不强制校验（指导评语/评阅评语可在中期完成后任意时间填写）
     */
    public Integer getRequiredStageStatus() {
        return switch (this) {
            case OPENING_SCORE, TRANSLATION -> 2;
            case MIDTERM_CHECK -> 2;
            case SUPERVISION_COMMENT, REVIEW_COMMENT -> null;
            case DEFENSE_RECORD, DEFENSE_SCORE -> 2;
            case COMMITTEE_EVALUATION -> 3;
        };
    }

    /**
     * 委员会评定加权权重
     *
     * @return 权重（如 0.12），null 表示不参与加权计算
     */
    public Double getWeight() {
        return switch (this) {
            case OPENING_SCORE -> 0.12;
            case TRANSLATION -> 0.03;
            case MIDTERM_CHECK -> 0.15;
            case SUPERVISION_COMMENT -> 0.15;
            case REVIEW_COMMENT -> 0.15;
            case DEFENSE_SCORE -> 0.40;
            default -> null;
        };
    }
}
