package com.example.graduationevaluationsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.graduationevaluationsystem.entity.Student;
import com.example.graduationevaluationsystem.entity.TeacherStudent;
import com.example.graduationevaluationsystem.mapper.StudentMapper;
import com.example.graduationevaluationsystem.mapper.TeacherStudentMapper;
import com.example.graduationevaluationsystem.service.TeacherStudentService;
import com.example.graduationevaluationsystem.vo.TeacherStudentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 师生关系 Service 实现
 */
@Service
@RequiredArgsConstructor
public class TeacherStudentServiceImpl extends ServiceImpl<TeacherStudentMapper, TeacherStudent> implements TeacherStudentService {

    private final StudentMapper studentMapper;

    @Override
    public List<TeacherStudentVO> getRelationList(String teacherNo, String studentNo, String relationType) {
        List<TeacherStudentVO> list = baseMapper.selectRelationList(teacherNo, studentNo, relationType);
        list.forEach(vo -> {
            Integer status = vo.getRelationStatus();
            if (status != null) {
                vo.setRelationStatusDesc(status == 1 ? "生效" : status == 2 ? "已解除" : "未知");
            }
        });
        return list;
    }

    @Override
    @Transactional
    public void updateRelationStatus(Integer id, Integer relationStatus) {
        TeacherStudent teacherStudent = getById(id);
        if (teacherStudent == null) {
            throw new RuntimeException("师生关系记录不存在");
        }
        teacherStudent.setRelationStatus(relationStatus);
        updateById(teacherStudent);
    }

    /**
     * 重写保存方法：指定指导教师时自动更新学生整体进度为"进行中"
     */
    @Override
    @Transactional
    public boolean save(TeacherStudent entity) {
        if (entity.getRelationStatus() == null) {
            entity.setRelationStatus(1);
        }
        boolean result = super.save(entity);

        // 指定指导教师后，学生整体进度自动变为 2（进行中）
        if ("指导".equals(entity.getRelationType()) && entity.getRelationStatus() == 1) {
            Student student = studentMapper.selectById(entity.getStudentNo());
            if (student != null && student.getOverallStatus() != null && student.getOverallStatus() == 1) {
                student.setOverallStatus(2);
                studentMapper.updateById(student);
            }
        }
        return result;
    }
}
