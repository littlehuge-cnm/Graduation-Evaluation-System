package com.example.graduationevaluationsystem.vo;

import lombok.Data;

/**
 * 学生的指导/评阅教师视图对象
 */
@Data
public class StudentTeacherVO {

    private TeacherBriefVO supervisor;
    private TeacherBriefVO reviewer;
}
