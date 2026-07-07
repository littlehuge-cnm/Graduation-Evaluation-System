package com.example.graduationevaluationsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.graduationevaluationsystem.entity.TeacherGroup;
import com.example.graduationevaluationsystem.vo.TeacherGroupVO;

import java.util.List;

/**
 * 教师分组 Service
 */
public interface TeacherGroupService extends IService<TeacherGroup> {

    /**
     * 查询全部分组（含教师姓名）
     *
     * @return 分组列表
     */
    List<TeacherGroupVO> getAllGroups();

    /**
     * 按编号查询分组详情（含教师姓名）
     *
     * @param groupId 分组编号
     * @return 分组详情
     */
    TeacherGroupVO getGroupById(Integer groupId);

    /**
     * 修改分组状态
     *
     * @param groupId     分组编号
     * @param groupStatus 分组状态：1=待启用/2=已启用/3=已停用
     */
    void updateGroupStatus(Integer groupId, Integer groupStatus);
}
