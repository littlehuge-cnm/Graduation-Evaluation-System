package com.example.graduationevaluationsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.graduationevaluationsystem.entity.TeacherGroup;
import com.example.graduationevaluationsystem.mapper.TeacherGroupMapper;
import com.example.graduationevaluationsystem.service.TeacherGroupService;
import com.example.graduationevaluationsystem.vo.TeacherGroupVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 教师分组 Service 实现
 */
@Service
public class TeacherGroupServiceImpl extends ServiceImpl<TeacherGroupMapper, TeacherGroup> implements TeacherGroupService {

    @Override
    public List<TeacherGroupVO> getAllGroups() {
        List<TeacherGroupVO> list = baseMapper.selectAllGroups();
        list.forEach(this::fillStatusDesc);
        return list;
    }

    @Override
    public TeacherGroupVO getGroupById(Integer groupId) {
        TeacherGroupVO vo = baseMapper.selectGroupById(groupId);
        if (vo != null) {
            fillStatusDesc(vo);
        }
        return vo;
    }

    @Override
    public void updateGroupStatus(Integer groupId, Integer groupStatus) {
        TeacherGroup group = getById(groupId);
        if (group == null) {
            throw new RuntimeException("教师分组不存在");
        }
        group.setGroupStatus(groupStatus);
        updateById(group);
    }

    /**
     * 填充分组状态描述
     */
    private void fillStatusDesc(TeacherGroupVO vo) {
        Integer status = vo.getGroupStatus();
        if (status != null) {
            vo.setGroupStatusDesc(switch (status) {
                case 1 -> "待启用";
                case 2 -> "已启用";
                case 3 -> "已停用";
                default -> "未知";
            });
        }
    }
}
