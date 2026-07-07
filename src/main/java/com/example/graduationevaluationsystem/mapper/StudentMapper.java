package com.example.graduationevaluationsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.graduationevaluationsystem.entity.Student;
import com.example.graduationevaluationsystem.vo.TeacherBriefVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 学生 Mapper
 */
@Mapper
public interface StudentMapper extends BaseMapper<Student> {

    /**
     * 按学号查询关联教师（指导/评阅）
     *
     * @param studentNo    学号
     * @param relationType 关系类型（指导/评阅）
     * @return 教师简要信息
     */
    @Select("""
            SELECT t.teacher_no, t.teacher_name
            FROM t_teacher_student ts
            INNER JOIN t_teacher t ON ts.teacher_no = t.teacher_no
            WHERE ts.student_no = #{studentNo}
              AND ts.relation_status = 1
              AND ts.relation_type = #{relationType}
            LIMIT 1
            """)
    TeacherBriefVO selectTeacherByStudentNo(@Param("studentNo") String studentNo,
                                            @Param("relationType") String relationType);
}
