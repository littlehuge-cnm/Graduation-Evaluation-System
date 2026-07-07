package com.example.graduationevaluationsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.graduationevaluationsystem.entity.GroupMapping;
import com.example.graduationevaluationsystem.entity.StudentGroup;
import com.example.graduationevaluationsystem.entity.TeacherGroup;
import com.example.graduationevaluationsystem.mapper.GroupMappingMapper;
import com.example.graduationevaluationsystem.mapper.StudentGroupMapper;
import com.example.graduationevaluationsystem.mapper.TeacherGroupMapper;
import com.example.graduationevaluationsystem.service.GroupMappingService;
import com.example.graduationevaluationsystem.vo.GroupMappingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 环节对应关系 Service 实现
 */
@Service
@RequiredArgsConstructor
public class GroupMappingServiceImpl extends ServiceImpl<GroupMappingMapper, GroupMapping> implements GroupMappingService {

    private final TeacherGroupMapper teacherGroupMapper;
    private final StudentGroupMapper studentGroupMapper;

    private static final Set<String> VALID_STAGES = Set.of("开题", "中期", "答辩");

    @Override
    public void createMapping(String stage, Integer teacherGroupId, Integer studentGroupId) {
        validateStage(stage);
        validateGroupsExist(teacherGroupId, studentGroupId);
        validateNotDuplicate(stage, teacherGroupId, studentGroupId, null);

        GroupMapping mapping = new GroupMapping();
        mapping.setStage(stage);
        mapping.setTeacherGroupId(teacherGroupId);
        mapping.setStudentGroupId(studentGroupId);
        save(mapping);
    }

    @Override
    public void updateMapping(Integer id, String stage, Integer teacherGroupId, Integer studentGroupId) {
        GroupMapping existing = getById(id);
        if (existing == null) {
            throw new RuntimeException("环节对应关系不存在");
        }
        validateStage(stage);
        validateGroupsExist(teacherGroupId, studentGroupId);
        validateNotDuplicate(stage, teacherGroupId, studentGroupId, id);

        existing.setStage(stage);
        existing.setTeacherGroupId(teacherGroupId);
        existing.setStudentGroupId(studentGroupId);
        updateById(existing);
    }

    @Override
    public List<GroupMappingVO> getListByStage(String stage) {
        return baseMapper.selectMappingList(stage);
    }

    @Override
    public GroupMappingVO getMappingById(Integer id) {
        return baseMapper.selectMappingById(id);
    }

    /**
     * 校验环节是否合法
     */
    private void validateStage(String stage) {
        if (!VALID_STAGES.contains(stage)) {
            throw new RuntimeException("环节不合法，仅支持：开题/中期/答辩");
        }
    }

    /**
     * 校验教师组和学生组是否存在
     */
    private void validateGroupsExist(Integer teacherGroupId, Integer studentGroupId) {
        if (teacherGroupMapper.selectById(teacherGroupId) == null) {
            throw new RuntimeException("教师分组不存在：" + teacherGroupId);
        }
        if (studentGroupMapper.selectById(studentGroupId) == null) {
            throw new RuntimeException("学生分组不存在：" + studentGroupId);
        }
    }

    /**
     * 校验同一环节下教师组/学生组是否已被占用（排除自身）
     */
    private void validateNotDuplicate(String stage, Integer teacherGroupId, Integer studentGroupId, Integer excludeId) {
        // 检查教师组在该环节下是否已对应其他学生组
        LambdaQueryWrapper<GroupMapping> teacherWrapper = new LambdaQueryWrapper<>();
        teacherWrapper.eq(GroupMapping::getStage, stage)
                      .eq(GroupMapping::getTeacherGroupId, teacherGroupId);
        if (excludeId != null) {
            teacherWrapper.ne(GroupMapping::getId, excludeId);
        }
        if (count(teacherWrapper) > 0) {
            throw new RuntimeException("该环节下教师组[" + teacherGroupId + "]已存在对应关系");
        }

        // 检查学生组在该环节下是否已对应其他教师组
        LambdaQueryWrapper<GroupMapping> studentWrapper = new LambdaQueryWrapper<>();
        studentWrapper.eq(GroupMapping::getStage, stage)
                      .eq(GroupMapping::getStudentGroupId, studentGroupId);
        if (excludeId != null) {
            studentWrapper.ne(GroupMapping::getId, excludeId);
        }
        if (count(studentWrapper) > 0) {
            throw new RuntimeException("该环节下学生组[" + studentGroupId + "]已存在对应关系");
        }
    }
}
