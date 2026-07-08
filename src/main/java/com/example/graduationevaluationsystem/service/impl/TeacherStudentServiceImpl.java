package com.example.graduationevaluationsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.graduationevaluationsystem.common.exception.BusinessException;
import com.example.graduationevaluationsystem.dto.TeacherStudentBatchAssignDTO;
import com.example.graduationevaluationsystem.entity.Student;
import com.example.graduationevaluationsystem.entity.TeacherStudent;
import com.example.graduationevaluationsystem.mapper.StudentMapper;
import com.example.graduationevaluationsystem.mapper.TeacherStudentMapper;
import com.example.graduationevaluationsystem.service.TeacherStudentService;
import com.example.graduationevaluationsystem.vo.TeacherStudentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 师生关系 Service 实现
 */
@Service
@RequiredArgsConstructor
public class TeacherStudentServiceImpl extends ServiceImpl<TeacherStudentMapper, TeacherStudent>
        implements TeacherStudentService {

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

    @Override
    @Transactional
    public void batchAssign(List<TeacherStudentBatchAssignDTO> list) {
        for (TeacherStudentBatchAssignDTO dto : list) {
            String studentNo = dto.getStudentNo();
            String guideTeacherNo = dto.getGuideTeacherNo();
            String reviewTeacherNo = dto.getReviewTeacherNo();

            Student student = studentMapper.selectById(studentNo);
            if (student == null) {
                throw new BusinessException("学生不存在：" + studentNo);
            }
            if (StringUtils.hasText(guideTeacherNo) && StringUtils.hasText(reviewTeacherNo)
                    && guideTeacherNo.equals(reviewTeacherNo)) {
                throw new BusinessException("学生" + student.getStudentName() + "（" + studentNo + "）的指导教师与评阅教师不能相同");
            }

            saveOrUpdateRelation(studentNo, "指导", guideTeacherNo);
            saveOrUpdateRelation(studentNo, "评阅", reviewTeacherNo);
        }
    }

    private void saveOrUpdateRelation(String studentNo, String relationType, String teacherNo) {
        LambdaQueryWrapper<TeacherStudent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeacherStudent::getStudentNo, studentNo)
                .eq(TeacherStudent::getRelationType, relationType);
        TeacherStudent exist = baseMapper.selectOne(wrapper);
        if (StringUtils.hasText(teacherNo)) {
            if (exist != null) {
                exist.setTeacherNo(teacherNo);
                exist.setRelationStatus(1);
                updateById(exist);
            } else {
                TeacherStudent relation = new TeacherStudent();
                relation.setStudentNo(studentNo);
                relation.setTeacherNo(teacherNo);
                relation.setRelationType(relationType);
                relation.setRelationStatus(1);
                save(relation);
            }
        } else if (exist != null) {
            exist.setRelationStatus(2);
            updateById(exist);
        }
    }
}
