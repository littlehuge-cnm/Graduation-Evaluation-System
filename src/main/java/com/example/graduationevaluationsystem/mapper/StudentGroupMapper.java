package com.example.graduationevaluationsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.graduationevaluationsystem.entity.StudentGroup;
import com.example.graduationevaluationsystem.vo.StudentGroupVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 学生分组 Mapper
 */
@Mapper
public interface StudentGroupMapper extends BaseMapper<StudentGroup> {

    /**
     * 查询全部分组（含组内学生数量，基于 student_no 字段统计）
     */
    @Select("""
            SELECT g.group_id, g.group_name, g.student_no,
                   IFNULL(LENGTH(g.student_no) - LENGTH(REPLACE(g.student_no, ',', '')) + 1, 0) AS student_count
            FROM t_student_group g
            ORDER BY g.group_id
            """)
    List<StudentGroupVO> selectAllGroups();

    /**
     * 按分组编号查询详情
     */
    @Select("""
            SELECT g.group_id, g.group_name, g.student_no
            FROM t_student_group g
            WHERE g.group_id = #{groupId}
            """)
    StudentGroupVO selectGroupById(@Param("groupId") Integer groupId);

    /**
     * 按学号列表查询学生简要信息
     */
    @Select("""
            <script>
            SELECT student_no, student_name, class_name, major, grade
            FROM t_student
            WHERE student_no IN
            <foreach collection="studentNos" item="no" open="(" separator="," close=")">
                #{no}
            </foreach>
            ORDER BY student_no
            </script>
            """)
    List<StudentGroupVO.StudentBriefVO> selectStudentsByNos(@Param("studentNos") List<String> studentNos);
}
