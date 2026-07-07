package com.example.graduationevaluationsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.graduationevaluationsystem.entity.StageStatus;
import com.example.graduationevaluationsystem.vo.StageStatusOverviewVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 环节状态 Mapper
 */
@Mapper
public interface StageStatusMapper extends BaseMapper<StageStatus> {

    /**
     * 查询指定学生组内所有学生在指定环节的状态
     * <p>
     * 左连接 t_student 以获取学生姓名，左连接 t_stage_status 获取环节状态。
     * 若学生尚无该环节记录，status 返回 null。
     *
     * @param studentGroupId 学生组号
     * @param stage          环节（开题/中期/答辩）
     * @return 学生环节状态明细列表
     */
    @Select("""
            SELECT s.student_no AS studentNo,
                   s.student_name AS studentName,
                   ss.status AS status
            FROM t_student s
            LEFT JOIN t_stage_status ss
                   ON ss.student_no = s.student_no AND ss.stage = #{stage}
            WHERE s.student_group_id = #{studentGroupId}
            ORDER BY s.student_no
            """)
    List<StageStatusOverviewVO.StudentStageStatusItem> selectOverviewByGroup(
            @Param("studentGroupId") Integer studentGroupId,
            @Param("stage") String stage);
}
