package com.example.graduationevaluationsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.graduationevaluationsystem.entity.Document;
import com.example.graduationevaluationsystem.vo.DocumentVO;
import com.example.graduationevaluationsystem.vo.MyStudentDocumentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 文档 Mapper
 */
@Mapper
public interface DocumentMapper extends BaseMapper<Document> {

    /**
     * 按编号查询文档详情（关联学生表获取姓名）
     *
     * @param id 记录编号
     * @return 文档详情
     */
    @Select("""
            SELECT d.id, d.student_no, s.student_name,
                   d.doc_type, d.title, d.subject_category, d.subject_type, d.subject_new_old,
                   d.content, d.status, d.approval_status,
                   d.submit_time, d.update_time
            FROM t_document d
            LEFT JOIN t_student s ON d.student_no = s.student_no
            WHERE d.id = #{id}
            """)
    DocumentVO selectDocById(@Param("id") Integer id);

    /**
     * 按学号查询文档列表（关联学生表获取姓名），可按文档类型过滤
     *
     * @param studentNo 学号
     * @param docType   文档类型（可为空）
     * @return 文档列表
     */
    @Select("""
            <script>
            SELECT d.id, d.student_no, s.student_name,
                   d.doc_type, d.title, d.subject_category, d.subject_type, d.subject_new_old,
                   d.content, d.status, d.approval_status,
                   d.submit_time, d.update_time
            FROM t_document d
            LEFT JOIN t_student s ON d.student_no = s.student_no
            WHERE d.student_no = #{studentNo}
            <if test="docType != null and docType != ''">
                AND d.doc_type = #{docType}
            </if>
            ORDER BY d.doc_type
            </script>
            """)
    List<DocumentVO> selectDocsByStudentNo(@Param("studentNo") String studentNo,
                                            @Param("docType") String docType);

    /**
     * 查询指导教师所指导学生的文档填写情况
     *
     * @param teacherNo 教师工号
     * @param docType   文档类型（可为空）
     * @return 学生文档列表
     */
    @Select("""
            <script>
            SELECT s.student_no, s.student_name, s.class_name, s.major,
                   d.id AS doc_id, d.doc_type, d.status, d.approval_status
            FROM t_student s
            INNER JOIN t_teacher_student ts ON s.student_no = ts.student_no
                AND ts.teacher_no = #{teacherNo}
                AND ts.relation_type = '指导'
                AND ts.relation_status = 1
            LEFT JOIN t_document d ON s.student_no = d.student_no
                <if test="docType != null and docType != ''">
                    AND d.doc_type = #{docType}
                </if>
            ORDER BY s.student_no
            </script>
            """)
    List<MyStudentDocumentVO> selectMyStudentDocs(@Param("teacherNo") String teacherNo,
                                                   @Param("docType") String docType);
}
