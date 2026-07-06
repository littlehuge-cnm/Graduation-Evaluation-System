package com.example.graduationevaluationsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.graduationevaluationsystem.entity.Teacher;
import com.example.graduationevaluationsystem.vo.TeacherStudentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 教师 Mapper
 */
@Mapper
public interface TeacherMapper extends BaseMapper<Teacher> {

    /**
     * 按教师工号查询关联学生列表
     *
     * @param teacherNo   教师工号
     * @param relationType 关系类型（指导/评阅），为 null 时查全部
     * @return 学生列表
     */
    @Select("""
            SELECT s.student_no, s.student_name, s.gender, s.class_name,
                   s.major, s.grade, s.student_group_id, ts.relation_type
            FROM t_teacher_student ts
            INNER JOIN t_student s ON ts.student_no = s.student_no
            WHERE ts.teacher_no = #{teacherNo}
              AND ts.relation_status = 1
              AND (#{relationType} IS NULL OR ts.relation_type = #{relationType})
            """)
    List<TeacherStudentVO> selectStudentsByTeacherNo(@Param("teacherNo") String teacherNo,
                                                     @Param("relationType") String relationType);
}
