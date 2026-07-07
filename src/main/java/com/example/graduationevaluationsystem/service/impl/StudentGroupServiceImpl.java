package com.example.graduationevaluationsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.graduationevaluationsystem.entity.Student;
import com.example.graduationevaluationsystem.entity.StudentGroup;
import com.example.graduationevaluationsystem.mapper.StudentGroupMapper;
import com.example.graduationevaluationsystem.mapper.StudentMapper;
import com.example.graduationevaluationsystem.service.StudentGroupService;
import com.example.graduationevaluationsystem.vo.StudentGroupVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 学生分组 Service 实现
 */
@Service
@RequiredArgsConstructor
public class StudentGroupServiceImpl extends ServiceImpl<StudentGroupMapper, StudentGroup> implements StudentGroupService {

    private final StudentMapper studentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer createGroup(String groupName, List<String> studentNos) {
        StudentGroup group = new StudentGroup();
        group.setGroupName(groupName);
        save(group);

        if (studentNos != null && !studentNos.isEmpty()) {
            bindStudents(group.getGroupId(), studentNos);
        }
        return group.getGroupId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGroup(Integer groupId, String groupName, List<String> studentNos) {
        StudentGroup group = getById(groupId);
        if (group == null) {
            throw new RuntimeException("学生分组不存在");
        }

        // 更新组名
        if (groupName != null) {
            group.setGroupName(groupName);
            updateById(group);
        }

        // 更新学生绑定
        if (studentNos != null) {
            // 先解除该组原有学生的绑定
            unbindAllStudents(groupId);
            // 再绑定新的学生
            if (!studentNos.isEmpty()) {
                bindStudents(groupId, studentNos);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGroup(Integer groupId) {
        StudentGroup group = getById(groupId);
        if (group == null) {
            throw new RuntimeException("学生分组不存在");
        }

        // 先解除组内学生的绑定
        unbindAllStudents(groupId);
        // 再删除分组
        removeById(groupId);
    }

    @Override
    public List<StudentGroupVO> getAllGroups() {
        return baseMapper.selectAllGroups();
    }

    @Override
    public StudentGroupVO getGroupById(Integer groupId) {
        StudentGroupVO vo = baseMapper.selectGroupById(groupId);
        if (vo == null) {
            return null;
        }
        List<StudentGroupVO.StudentBriefVO> students = baseMapper.selectStudentsByGroupId(groupId);
        vo.setStudents(students);
        return vo;
    }

    /**
     * 将指定学号列表的学生绑定到分组
     */
    private void bindStudents(Integer groupId, List<String> studentNos) {
        LambdaUpdateWrapper<Student> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(Student::getStudentNo, studentNos)
               .set(Student::getStudentGroupId, groupId);
        studentMapper.update(null, wrapper);
    }

    /**
     * 解除分组下所有学生的绑定
     */
    private void unbindAllStudents(Integer groupId) {
        LambdaUpdateWrapper<Student> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Student::getStudentGroupId, groupId)
               .set(Student::getStudentGroupId, null);
        studentMapper.update(null, wrapper);
    }
}
