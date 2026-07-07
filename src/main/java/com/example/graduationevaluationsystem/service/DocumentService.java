package com.example.graduationevaluationsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.graduationevaluationsystem.entity.Document;
import com.example.graduationevaluationsystem.vo.DocumentVO;
import com.example.graduationevaluationsystem.vo.MyStudentDocumentVO;

import java.util.List;

/**
 * 文档 Service
 */
public interface DocumentService extends IService<Document> {

    /**
     * 保存/更新文档
     *
     * @param studentNo       学号
     * @param docType         文档类型（任务书/指导书）
     * @param title           题目
     * @param subjectCategory 课题类别
     * @param subjectType     课题类型
     * @param subjectNewOld   新旧课题
     * @param content         正文内容
     * @param status          文档状态（1=草稿/2=已提交）
     * @return 文档编号
     */
    Integer saveDocument(String studentNo, String docType, String title,
                         String subjectCategory, String subjectType, String subjectNewOld,
                         String content, Integer status);

    /**
     * 修改文档
     *
     * @param id              记录编号
     * @param studentNo       学号
     * @param docType         文档类型
     * @param title           题目
     * @param subjectCategory 课题类别
     * @param subjectType     课题类型
     * @param subjectNewOld   新旧课题
     * @param content         正文内容
     * @param status          文档状态
     */
    void updateDocument(Integer id, String studentNo, String docType, String title,
                        String subjectCategory, String subjectType, String subjectNewOld,
                        String content, Integer status);

    /**
     * 提交文档（草稿→已提交，任务书自动进入待系审）
     *
     * @param id 记录编号
     */
    void submitDocument(Integer id);

    /**
     * 退回文档（已提交→草稿，清空审批状态）
     *
     * @param id 记录编号
     */
    void rollbackDocument(Integer id);

    /**
     * 系主任审核（超管代行）
     *
     * @param id             记录编号
     * @param approvalStatus 审批状态（2=系通过→自动转为4待院审 / 3=系驳回）
     */
    void deptApproval(Integer id, Integer approvalStatus);

    /**
     * 院长审核（超管代行）
     *
     * @param id             记录编号
     * @param approvalStatus 审批状态（5=院通过 / 6=院驳回）
     */
    void collegeApproval(Integer id, Integer approvalStatus);

    /**
     * 按编号查询文档详情（含学生姓名、状态描述）
     *
     * @param id 记录编号
     * @return 文档详情
     */
    DocumentVO getDocById(Integer id);

    /**
     * 按学号查询文档列表
     *
     * @param studentNo 学号
     * @param docType   文档类型（可为空）
     * @return 文档列表
     */
    List<DocumentVO> getDocsByStudentNo(String studentNo, String docType);

    /**
     * 查询指导教师所指导学生的文档填写情况
     *
     * @param teacherNo 教师工号
     * @param docType   文档类型（可为空）
     * @return 学生文档列表
     */
    List<MyStudentDocumentVO> getMyStudentDocs(String teacherNo, String docType);
}
