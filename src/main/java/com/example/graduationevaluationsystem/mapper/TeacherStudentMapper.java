package com.example.graduationevaluationsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.graduationevaluationsystem.entity.TeacherStudent;
import com.example.graduationevaluationsystem.vo.TeacherStudentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 师生关系 Mapper
 */
@Mapper
public interface TeacherStudentMapper extends BaseMapper<TeacherStudent> {

    /**
     * 按条件查询师生关系列表（关联教师表和学生表获取姓名）
     *
     * @param teacherNo   教师工号（可为空）
     * @param studentNo   学号（可为空）
     * @param relationType 关系类型（可为空）
     * @return 师生关系列表
     */
    @Select("""
            <script>
            SELECT ts.id, ts.student_no, s.student_name, ts.teacher_no, t.teacher_name,
                   ts.relation_type, ts.relation_status
            FROM t_teacher_student ts
            LEFT JOIN t_student s ON ts.student_no = s.student_no
            LEFT JOIN t_teacher t ON ts.teacher_no = t.teacher_no
            <where>
                <if test="teacherNo != null and teacherNo != ''">
                    AND ts.teacher_no = #{teacherNo}
                </if>
                <if test="studentNo != null and studentNo != ''">
                    AND ts.student_no = #{studentNo}
                </if>
                <if test="relationType != null and relationType != ''">
                    AND ts.relation_type = #{relationType}
                </if>
            </where>
            ORDER BY ts.id
            </script>
            """)
    List<TeacherStudentVO> selectRelationList(@Param("teacherNo") String teacherNo,
                                              @Param("studentNo") String studentNo,
                                              @Param("relationType") String relationType);
}
