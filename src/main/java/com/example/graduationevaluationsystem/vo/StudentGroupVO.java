package com.example.graduationevaluationsystem.vo;

import lombok.Data;

import java.util.List;

/**
 * 学生分组视图对象（含组内学生列表）
 */
@Data
public class StudentGroupVO {

    private Integer groupId;
    private String groupName;
    private Integer studentCount;

    /**
     * 组内学生简要信息列表
     */
    private List<StudentBriefVO> students;

    /**
     * 学生简要信息
     */
    @Data
    public static class StudentBriefVO {
        private String studentNo;
        private String studentName;
        private String className;
        private String major;
        private String grade;
    }
}
