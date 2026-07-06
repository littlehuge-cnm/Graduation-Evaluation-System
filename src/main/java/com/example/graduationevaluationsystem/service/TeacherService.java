package com.example.graduationevaluationsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.graduationevaluationsystem.entity.Teacher;
import com.example.graduationevaluationsystem.vo.TeacherStudentVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 教师 Service
 */
public interface TeacherService extends IService<Teacher> {

    /**
     * 批量导入教师
     *
     * @param file Excel/CSV 文件
     * @return 导入成功的数量
     */
    int importTeachers(MultipartFile file);

    /**
     * 按教师工号查询关联学生列表
     *
     * @param teacherNo   教师工号
     * @param relationType 关系类型（指导/评阅），为 null 时查全部
     * @return 学生列表
     */
    List<TeacherStudentVO> getStudentsByTeacherNo(String teacherNo, String relationType);
}
