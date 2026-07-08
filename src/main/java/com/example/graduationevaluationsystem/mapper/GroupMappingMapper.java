package com.example.graduationevaluationsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.graduationevaluationsystem.entity.GroupMapping;
import com.example.graduationevaluationsystem.vo.GroupMappingVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 环节对应关系 Mapper
 */
@Mapper
public interface GroupMappingMapper extends BaseMapper<GroupMapping> {

    /**
     * 查询全部对应关系（含教师组名、学生组名），可按环节过滤
     *
     * @param stage 环节（为 null 时查询全部）
     * @return 对应关系列表
     */
    @Select("""
            <script>
            SELECT m.id, m.stage,
                   m.teacher_group_id, tg.group_name AS teacher_group_name,
                   m.student_group_id, sg.group_name AS student_group_name
            FROM t_group_mapping m
            LEFT JOIN t_teacher_group tg ON m.teacher_group_id = tg.group_id
            LEFT JOIN t_student_group sg ON m.student_group_id = sg.group_id
            <if test="stage != null and stage != ''">
                WHERE m.stage = #{stage}
            </if>
            ORDER BY m.stage, m.teacher_group_id
            </script>
            """)
    List<GroupMappingVO> selectMappingList(@Param("stage") String stage);

    /**
     * 按编号查询对应关系详情（含教师组名、学生组名）
     *
     * @param id 记录编号
     * @return 对应关系详情
     */
    @Select("""
            SELECT m.id, m.stage,
                   m.teacher_group_id, tg.group_name AS teacher_group_name,
                   m.student_group_id, sg.group_name AS student_group_name
            FROM t_group_mapping m
            LEFT JOIN t_teacher_group tg ON m.teacher_group_id = tg.group_id
            LEFT JOIN t_student_group sg ON m.student_group_id = sg.group_id
            WHERE m.id = #{id}
            """)
    GroupMappingVO selectMappingById(@Param("id") Integer id);

    /**
     * 清空全部对应关系
     *
     * @return 受影响行数
     */
    @Delete("DELETE FROM t_group_mapping")
    int deleteAll();
}
