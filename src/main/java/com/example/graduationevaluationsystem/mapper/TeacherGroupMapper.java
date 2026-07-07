package com.example.graduationevaluationsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.graduationevaluationsystem.entity.TeacherGroup;
import com.example.graduationevaluationsystem.vo.TeacherGroupVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 教师分组 Mapper
 */
@Mapper
public interface TeacherGroupMapper extends BaseMapper<TeacherGroup> {

    /**
     * 查询全部分组（关联教师表获取姓名）
     */
    @Select("""
            SELECT g.group_id, g.group_name,
                   g.leader_no,    lt.teacher_name AS leader_name,
                   g.secretary_no, st.teacher_name AS secretary_name,
                   g.member_no,    mt.teacher_name AS member_name,
                   g.group_status
            FROM t_teacher_group g
            LEFT JOIN t_teacher lt ON g.leader_no    = lt.teacher_no
            LEFT JOIN t_teacher st ON g.secretary_no = st.teacher_no
            LEFT JOIN t_teacher mt ON g.member_no    = mt.teacher_no
            ORDER BY g.group_id
            """)
    List<TeacherGroupVO> selectAllGroups();

    /**
     * 按分组编号查询详情（关联教师表获取姓名）
     */
    @Select("""
            SELECT g.group_id, g.group_name,
                   g.leader_no,    lt.teacher_name AS leader_name,
                   g.secretary_no, st.teacher_name AS secretary_name,
                   g.member_no,    mt.teacher_name AS member_name,
                   g.group_status
            FROM t_teacher_group g
            LEFT JOIN t_teacher lt ON g.leader_no    = lt.teacher_no
            LEFT JOIN t_teacher st ON g.secretary_no = st.teacher_no
            LEFT JOIN t_teacher mt ON g.member_no    = mt.teacher_no
            WHERE g.group_id = #{groupId}
            """)
    TeacherGroupVO selectGroupById(@Param("groupId") Integer groupId);
}
