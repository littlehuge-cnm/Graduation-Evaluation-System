package com.example.graduationevaluationsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.graduationevaluationsystem.entity.ScoreRecord;
import com.example.graduationevaluationsystem.vo.ScoreRecordTodoVO;
import com.example.graduationevaluationsystem.vo.ScoreRecordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 评价记录 Mapper
 */
@Mapper
public interface ScoreRecordMapper extends BaseMapper<ScoreRecord> {

    /**
     * 按编号查询评价记录详情（关联教师表获取录入人姓名）
     */
    @Select("""
            SELECT r.id, r.student_no, r.item_type, r.sub_scores, r.score, r.grade,
                   r.comment, r.defense_record, r.recorder_no,
                   COALESCE(t.teacher_name, a.admin_name) AS recorder_name,
                   r.record_time, r.update_time, r.record_status
            FROM t_score_record r
            LEFT JOIN t_teacher t ON r.recorder_no = t.teacher_no
            LEFT JOIN t_admin a ON r.recorder_no = a.admin_id
            WHERE r.id = #{id}
            """)
    ScoreRecordVO selectRecordById(@Param("id") Integer id);

    /**
     * 按学号查询评价记录列表，可按条目类型过滤
     */
    @Select("""
            <script>
            SELECT r.id, r.student_no, r.item_type, r.sub_scores, r.score, r.grade,
                   r.comment, r.defense_record, r.recorder_no,
                   COALESCE(t.teacher_name, a.admin_name) AS recorder_name,
                   r.record_time, r.update_time, r.record_status
            FROM t_score_record r
            LEFT JOIN t_teacher t ON r.recorder_no = t.teacher_no
            LEFT JOIN t_admin a ON r.recorder_no = a.admin_id
            WHERE r.student_no = #{studentNo}
            <if test="itemType != null and itemType != ''">
                AND r.item_type = #{itemType}
            </if>
            ORDER BY r.record_time
            </script>
            """)
    List<ScoreRecordVO> selectRecordsByStudentNo(@Param("studentNo") String studentNo,
                                                  @Param("itemType") String itemType);

    /**
     * 查询学生指定环节的状态码
     *
     * @param studentNo 学号
     * @param stage     环节（开题/中期/答辩）
     * @return 状态码（1=未开始/2=进行中/3=已完成），null 表示无记录
     */
    @Select("SELECT status FROM t_stage_status WHERE student_no = #{studentNo} AND stage = #{stage}")
    Integer selectStageStatus(@Param("studentNo") String studentNo, @Param("stage") String stage);

    /**
     * 查询学生全部已确认成绩记录（item_type → score 映射），用于委员会评定加权计算
     *
     * @param studentNo 学号
     * @return item_type 和 score 的映射列表
     */
    @Select("""
            SELECT item_type, score
            FROM t_score_record
            WHERE student_no = #{studentNo} AND record_status = 2 AND score IS NOT NULL
            """)
    List<Map<String, Object>> selectConfirmedScores(@Param("studentNo") String studentNo);

    /**
     * 查询教师的关联学生及其角色（组长/秘书/指导教师/评阅教师）
     * <p>
     * 通过 UNION 合并四种角色的查询结果。
     * 组长/秘书：通过 t_group_mapping → t_teacher_group 关联；
     * 指导教师/评阅教师：通过 t_teacher_student 关联。
     *
     * @param teacherNo 教师工号
     * @return 学生与角色列表
     */
    @Select("""
            <script>
            SELECT s.student_no, s.student_name, s.class_name, '组长' AS role
            FROM t_student s
            INNER JOIN t_group_mapping m ON s.student_group_id = m.student_group_id
            INNER JOIN t_teacher_group g ON m.teacher_group_id = g.group_id
            WHERE g.leader_no = #{teacherNo}
            UNION ALL
            SELECT s.student_no, s.student_name, s.class_name, '秘书' AS role
            FROM t_student s
            INNER JOIN t_group_mapping m ON s.student_group_id = m.student_group_id
            INNER JOIN t_teacher_group g ON m.teacher_group_id = g.group_id
            WHERE g.secretary_no = #{teacherNo}
            UNION ALL
            SELECT s.student_no, s.student_name, s.class_name, '指导教师' AS role
            FROM t_student s
            INNER JOIN t_teacher_student ts ON s.student_no = ts.student_no
            WHERE ts.teacher_no = #{teacherNo} AND ts.relation_type = '指导' AND ts.relation_status = 1
            UNION ALL
            SELECT s.student_no, s.student_name, s.class_name, '评阅教师' AS role
            FROM t_student s
            INNER JOIN t_teacher_student ts ON s.student_no = ts.student_no
            WHERE ts.teacher_no = #{teacherNo} AND ts.relation_type = '评阅' AND ts.relation_status = 1
            </script>
            """)
    List<ScoreRecordTodoVO> selectTeacherStudentsWithRoles(@Param("teacherNo") String teacherNo);

    /**
     * 查询需要委员会评定但尚未评定的学生
     * <p>
     * 条件：六项成绩（开题/翻译/中期/指导/评阅/答辩）均已确认，且尚无委员会评定记录。
     *
     * @return 待委员会评定的学生列表
     */
    @Select("""
            SELECT s.student_no, s.student_name, s.class_name, '委员会评定' AS item_type, '超级管理员' AS recorder_role
            FROM t_student s
            WHERE NOT EXISTS (
                SELECT 1 FROM t_score_record r
                WHERE r.student_no = s.student_no AND r.item_type = '委员会评定'
            )
            AND (
                SELECT COUNT(*) FROM t_score_record r
                WHERE r.student_no = s.student_no
                AND r.item_type IN ('开题成绩', '外文翻译', '中期检查', '指导评语', '评阅评语', '答辩成绩')
                AND r.record_status = 2
            ) = 6
            ORDER BY s.student_no
            """)
    List<ScoreRecordTodoVO> selectCommitteeTodoStudents();
}
