package com.example.graduationevaluationsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.graduationevaluationsystem.entity.TeacherStudent;
import com.example.graduationevaluationsystem.vo.TeacherStudentVO;

import java.util.List;

/**
 * 师生关系 Service
 */
public interface TeacherStudentService extends IService<TeacherStudent> {

    /**
     * 按条件查询师生关系列表（含教师姓名、学生姓名）
     *
     * @param teacherNo   教师工号（可为空）
     * @param studentNo   学号（可为空）
     * @param relationType 关系类型（可为空）
     * @return 师生关系列表
     */
    List<TeacherStudentVO> getRelationList(String teacherNo, String studentNo, String relationType);

    /**
     * 修改师生关系状态
     *
     * @param id             记录编号
     * @param relationStatus 关系状态：1=生效/2=已解除
     */
    void updateRelationStatus(Integer id, Integer relationStatus);
}
