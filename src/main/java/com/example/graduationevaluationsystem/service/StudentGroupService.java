package com.example.graduationevaluationsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.graduationevaluationsystem.entity.StudentGroup;
import com.example.graduationevaluationsystem.vo.StudentGroupVO;

import java.util.List;

/**
 * 学生分组 Service
 */
public interface StudentGroupService extends IService<StudentGroup> {

    /**
     * 创建学生分组，并将传入的学生划入该组
     *
     * @param groupName  组名
     * @param studentNos 学号列表
     * @return 新建的分组编号
     */
    Integer createGroup(String groupName, List<String> studentNos);

    /**
     * 修改学生分组（更新组名，并重新绑定组内学生）
     *
     * @param groupId    分组编号
     * @param groupName  组名
     * @param studentNos 学号列表（为 null 时不更新学生绑定）
     */
    void updateGroup(Integer groupId, String groupName, List<String> studentNos);

    /**
     * 删除学生分组（先将组内学生的 student_group_id 置空，再删除分组）
     *
     * @param groupId 分组编号
     */
    void deleteGroup(Integer groupId);

    /**
     * 查询全部分组（含学生数量）
     *
     * @return 分组列表
     */
    List<StudentGroupVO> getAllGroups();

    /**
     * 按编号查询分组详情（含组内学生列表）
     *
     * @param groupId 分组编号
     * @return 分组详情
     */
    StudentGroupVO getGroupById(Integer groupId);
}
