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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 学生分组 Service 实现
 */
@Service
@RequiredArgsConstructor
public class StudentGroupServiceImpl extends ServiceImpl<StudentGroupMapper, StudentGroup>
        implements StudentGroupService {

    private final StudentMapper studentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer createGroup(String groupName, List<String> studentNos) {
        StudentGroup group = new StudentGroup();
        group.setGroupName(groupName);
        // 将学号列表转为逗号分隔字符串存入学生组表
        group.setStudentNo(joinStudentNos(studentNos));
        save(group);

        // 同步更新学生表的 student_group_id（仅作关联）
        if (studentNos != null && !studentNos.isEmpty()) {
            // 把学生从之前的其他分组中移除，再绑定到新分组
            removeStudentsFromOldGroups(studentNos, group.getGroupId());
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
        }

        // 更新组内学号（在学生组表上直接操作）
        if (studentNos != null) {
            List<String> oldStudentNos = parseStudentNos(group.getStudentNo());
            List<String> newStudentNos = new ArrayList<>(studentNos);

            // 先把新学生从其他旧分组中移除（必须在 unbind 之前查，否则会丢失原分组信息）
            removeStudentsFromOldGroups(newStudentNos, groupId);

            // 解除当前分组中不再需要的学生关联
            List<String> toUnbind = oldStudentNos.stream()
                    .filter(s -> !newStudentNos.contains(s))
                    .toList();
            if (!toUnbind.isEmpty()) {
                unbindStudents(toUnbind);
            }

            // 更新学生组表的 student_no 字段
            group.setStudentNo(joinStudentNos(newStudentNos));
            // 绑定新学生的 student_group_id
            if (!newStudentNos.isEmpty()) {
                bindStudents(groupId, newStudentNos);
            }
        }

        updateById(group);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGroup(Integer groupId) {
        StudentGroup group = getById(groupId);
        if (group == null) {
            throw new RuntimeException("学生分组不存在");
        }

        // 先解除组内学生的关联（根据 student_no 字段）
        List<String> studentNos = parseStudentNos(group.getStudentNo());
        if (!studentNos.isEmpty()) {
            unbindStudents(studentNos);
        }

        // 再删除分组
        removeById(groupId);
    }

    @Override
    public List<StudentGroupVO> getAllGroups() {
        List<StudentGroupVO> groups = baseMapper.selectAllGroups();
        groups.forEach(this::populateGroupStudents);
        return groups;
    }

    @Override
    public StudentGroupVO getGroupById(Integer groupId) {
        StudentGroupVO vo = baseMapper.selectGroupById(groupId);
        if (vo == null) {
            return null;
        }
        populateGroupStudents(vo);
        return vo;
    }

    private void populateGroupStudents(StudentGroupVO vo) {
        List<String> studentNos = parseStudentNos(vo.getStudentNo());
        if (studentNos.isEmpty()) {
            vo.setStudents(new ArrayList<>());
        } else {
            List<StudentGroupVO.StudentBriefVO> students = baseMapper.selectStudentsByNos(studentNos);
            vo.setStudents(students != null ? students : new ArrayList<>());
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 将学号列表转为逗号分隔字符串
     */
    private String joinStudentNos(List<String> studentNos) {
        if (studentNos == null || studentNos.isEmpty()) {
            return null;
        }
        return String.join(",", studentNos);
    }

    /**
     * 将逗号分隔的学号字符串解析为列表
     */
    private List<String> parseStudentNos(String studentNo) {
        if (studentNo == null || studentNo.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(studentNo.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * 将指定学号列表的学生绑定到分组（更新 student_group_id）
     */
    private void bindStudents(Integer groupId, List<String> studentNos) {
        LambdaUpdateWrapper<Student> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(Student::getStudentNo, studentNos)
                .set(Student::getStudentGroupId, groupId);
        studentMapper.update(null, wrapper);
    }

    /**
     * 解除指定学号列表学生的分组绑定（置空 student_group_id）
     */
    private void unbindStudents(List<String> studentNos) {
        LambdaUpdateWrapper<Student> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(Student::getStudentNo, studentNos)
                .set(Student::getStudentGroupId, null);
        studentMapper.update(null, wrapper);
    }

    /**
     * 将指定学号列表的学生从他们所在的其他分组中移除
     */
    private void removeStudentsFromOldGroups(List<String> studentNos, Integer excludeGroupId) {
        if (studentNos == null || studentNos.isEmpty()) {
            return;
        }
        List<Student> existingStudents = studentMapper.selectBatchIds(studentNos);
        if (existingStudents == null) {
            return;
        }
        for (Student student : existingStudents) {
            Integer oldGroupId = student.getStudentGroupId();
            if (oldGroupId != null && !oldGroupId.equals(excludeGroupId)) {
                removeStudentNoFromGroup(oldGroupId, student.getStudentNo());
            }
        }
    }

    /**
     * 从指定分组的 student_no 中移除某个学号
     */
    private void removeStudentNoFromGroup(Integer groupId, String studentNo) {
        StudentGroup group = getById(groupId);
        if (group == null || group.getStudentNo() == null || group.getStudentNo().isBlank()) {
            return;
        }
        List<String> nos = new ArrayList<>(parseStudentNos(group.getStudentNo()));
        nos.remove(studentNo);
        group.setStudentNo(joinStudentNos(nos));
        updateById(group);
    }
}
