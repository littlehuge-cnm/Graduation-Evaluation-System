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
     * 查询全部分组（含组内学生数量）
     */
    @Select("""
            SELECT g.group_id, g.group_name,
                   (SELECT COUNT(*) FROM t_student s WHERE s.student_group_id = g.group_id) AS student_count
            FROM t_student_group g
            ORDER BY g.group_id
            """)
    List<StudentGroupVO> selectAllGroups();

    /**
     * 按分组编号查询详情（含组内学生数量）
     */
    @Select("""
            SELECT g.group_id, g.group_name,
                   (SELECT COUNT(*) FROM t_student s WHERE s.student_group_id = g.group_id) AS student_count
            FROM t_student_group g
            WHERE g.group_id = #{groupId}
            """)
    StudentGroupVO selectGroupById(@Param("groupId") Integer groupId);

    /**
     * 按分组编号查询组内学生列表
     */
    @Select("""
            SELECT student_no, student_name, class_name, major, grade
            FROM t_student
            WHERE student_group_id = #{groupId}
            ORDER BY student_no
            """)
    List<StudentGroupVO.StudentBriefVO> selectStudentsByGroupId(@Param("groupId") Integer groupId);
}
