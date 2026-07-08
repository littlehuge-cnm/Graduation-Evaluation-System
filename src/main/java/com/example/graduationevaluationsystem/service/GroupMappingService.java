package com.example.graduationevaluationsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.graduationevaluationsystem.entity.GroupMapping;
import com.example.graduationevaluationsystem.vo.GroupMappingVO;

import java.util.List;

/**
 * 环节对应关系 Service
 */
public interface GroupMappingService extends IService<GroupMapping> {

    /**
     * 创建环节对应关系（含重复校验）
     *
     * @param stage           环节
     * @param teacherGroupId  教师组号
     * @param studentGroupId  学生组号
     */
    void createMapping(String stage, Integer teacherGroupId, Integer studentGroupId);

    /**
     * 修改环节对应关系（含重复校验）
     *
     * @param id              记录编号
     * @param stage           环节
     * @param teacherGroupId  教师组号
     * @param studentGroupId  学生组号
     */
    void updateMapping(Integer id, String stage, Integer teacherGroupId, Integer studentGroupId);

    /**
     * 按环节查询对应关系列表（含组名）
     *
     * @param stage 环节（为 null 时查询全部）
     * @return 对应关系列表
     */
    List<GroupMappingVO> getListByStage(String stage);

    /**
     * 按编号查询对应关系详情（含组名）
     *
     * @param id 记录编号
     * @return 对应关系详情
     */
    GroupMappingVO getMappingById(Integer id);

    /**
     * 随机分配教师组给学生组（覆盖原有分配）
     * <p>
     * 一次性为三个环节（开题/中期/答辩）随机分配教师组给学生组：
     * - 同一环节中，不同学生组分配不同教师组（一对一）
     * - 同一学生组在三个环节中分配的教师组互不相同
     * - 先清除所有已有对应关系，再重新分配
     *
     * @return 分配结果列表（含教师组名、学生组名）
     */
    List<GroupMappingVO> randomAssignAll();
}
