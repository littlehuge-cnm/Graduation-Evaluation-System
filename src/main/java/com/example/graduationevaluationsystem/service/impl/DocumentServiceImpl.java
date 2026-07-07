package com.example.graduationevaluationsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.graduationevaluationsystem.common.enums.ApprovalStatus;
import com.example.graduationevaluationsystem.common.enums.DocumentStatus;
import com.example.graduationevaluationsystem.entity.Document;
import com.example.graduationevaluationsystem.mapper.DocumentMapper;
import com.example.graduationevaluationsystem.service.DocumentService;
import com.example.graduationevaluationsystem.vo.DocumentVO;
import com.example.graduationevaluationsystem.vo.MyStudentDocumentVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 文档 Service 实现
 */
@Service
public class DocumentServiceImpl extends ServiceImpl<DocumentMapper, Document> implements DocumentService {

    private static final Set<String> VALID_DOC_TYPES = Set.of("任务书", "指导书");

    @Override
    public Integer saveDocument(String studentNo, String docType, String title,
                                String subjectCategory, String subjectType, String subjectNewOld,
                                String content, Integer status) {
        validateDocType(docType);
        validateTitle(docType, title);
        validateStatus(status);

        Document doc = new Document();
        doc.setStudentNo(studentNo);
        doc.setDocType(docType);
        doc.setTitle(title);
        doc.setSubjectCategory(subjectCategory);
        doc.setSubjectType(subjectType);
        doc.setSubjectNewOld(subjectNewOld);
        doc.setContent(content);
        doc.setStatus(status);
        doc.setUpdateTime(LocalDateTime.now());
        // 提交时设置提交时间和审批状态
        if (status == DocumentStatus.SUBMITTED.getCode()) {
            doc.setSubmitTime(LocalDateTime.now());
            if ("任务书".equals(docType)) {
                doc.setApprovalStatus(ApprovalStatus.DEPT_PENDING.getCode());
            }
        }
        save(doc);
        return doc.getId();
    }

    @Override
    public void updateDocument(Integer id, String studentNo, String docType, String title,
                               String subjectCategory, String subjectType, String subjectNewOld,
                               String content, Integer status) {
        Document existing = getById(id);
        if (existing == null) {
            throw new RuntimeException("文档不存在");
        }
        // 已提交文档不可修改（需超管退回后方可修改）
        if (existing.getStatus() == DocumentStatus.SUBMITTED.getCode()) {
            throw new RuntimeException("文档已提交，不可修改，如需修改请联系管理员退回");
        }

        validateDocType(docType);
        validateTitle(docType, title);
        validateStatus(status);

        existing.setStudentNo(studentNo);
        existing.setDocType(docType);
        existing.setTitle(title);
        existing.setSubjectCategory(subjectCategory);
        existing.setSubjectType(subjectType);
        existing.setSubjectNewOld(subjectNewOld);
        existing.setContent(content);
        existing.setStatus(status);
        existing.setUpdateTime(LocalDateTime.now());
        // 草稿改为提交时设置提交时间和审批状态
        if (status == DocumentStatus.SUBMITTED.getCode()) {
            existing.setSubmitTime(LocalDateTime.now());
            if ("任务书".equals(docType)) {
                existing.setApprovalStatus(ApprovalStatus.DEPT_PENDING.getCode());
            }
        }
        updateById(existing);
    }

    @Override
    public void submitDocument(Integer id) {
        Document existing = getById(id);
        if (existing == null) {
            throw new RuntimeException("文档不存在");
        }
        if (existing.getStatus() != DocumentStatus.DRAFT.getCode()) {
            throw new RuntimeException("仅草稿状态的文档可以提交");
        }

        existing.setStatus(DocumentStatus.SUBMITTED.getCode());
        existing.setSubmitTime(LocalDateTime.now());
        existing.setUpdateTime(LocalDateTime.now());
        // 任务书提交后进入待系审
        if ("任务书".equals(existing.getDocType())) {
            existing.setApprovalStatus(ApprovalStatus.DEPT_PENDING.getCode());
        }
        updateById(existing);
    }

    @Override
    public void rollbackDocument(Integer id) {
        Document existing = getById(id);
        if (existing == null) {
            throw new RuntimeException("文档不存在");
        }
        if (existing.getStatus() != DocumentStatus.SUBMITTED.getCode()) {
            throw new RuntimeException("仅已提交的文档可以退回");
        }

        existing.setStatus(DocumentStatus.DRAFT.getCode());
        existing.setApprovalStatus(null);
        existing.setUpdateTime(LocalDateTime.now());
        updateById(existing);
    }

    @Override
    public void deptApproval(Integer id, Integer approvalStatus) {
        Document existing = getById(id);
        if (existing == null) {
            throw new RuntimeException("文档不存在");
        }
        if (!"任务书".equals(existing.getDocType())) {
            throw new RuntimeException("仅任务书支持课题审批");
        }
        if (existing.getApprovalStatus() == null
                || existing.getApprovalStatus() != ApprovalStatus.DEPT_PENDING.getCode()) {
            throw new RuntimeException("当前审批状态不支持系审操作");
        }

        // 2=系通过 → 自动流转为 4=待院审；3=系驳回
        if (approvalStatus == ApprovalStatus.DEPT_APPROVED.getCode()) {
            existing.setApprovalStatus(ApprovalStatus.COLLEGE_PENDING.getCode());
        } else if (approvalStatus == ApprovalStatus.DEPT_REJECTED.getCode()) {
            existing.setApprovalStatus(ApprovalStatus.DEPT_REJECTED.getCode());
        } else {
            throw new RuntimeException("系审仅支持：2=系通过 / 3=系驳回");
        }
        existing.setUpdateTime(LocalDateTime.now());
        updateById(existing);
    }

    @Override
    public void collegeApproval(Integer id, Integer approvalStatus) {
        Document existing = getById(id);
        if (existing == null) {
            throw new RuntimeException("文档不存在");
        }
        if (!"任务书".equals(existing.getDocType())) {
            throw new RuntimeException("仅任务书支持课题审批");
        }
        if (existing.getApprovalStatus() == null
                || existing.getApprovalStatus() != ApprovalStatus.COLLEGE_PENDING.getCode()) {
            throw new RuntimeException("当前审批状态不支持院审操作");
        }

        // 5=院通过（终态） / 6=院驳回
        if (approvalStatus == ApprovalStatus.COLLEGE_APPROVED.getCode()) {
            existing.setApprovalStatus(ApprovalStatus.COLLEGE_APPROVED.getCode());
        } else if (approvalStatus == ApprovalStatus.COLLEGE_REJECTED.getCode()) {
            existing.setApprovalStatus(ApprovalStatus.COLLEGE_REJECTED.getCode());
        } else {
            throw new RuntimeException("院审仅支持：5=院通过 / 6=院驳回");
        }
        existing.setUpdateTime(LocalDateTime.now());
        updateById(existing);
    }

    @Override
    public DocumentVO getDocById(Integer id) {
        DocumentVO vo = baseMapper.selectDocById(id);
        if (vo != null) {
            fillStatusDesc(vo);
        }
        return vo;
    }

    @Override
    public List<DocumentVO> getDocsByStudentNo(String studentNo, String docType) {
        List<DocumentVO> list = baseMapper.selectDocsByStudentNo(studentNo, docType);
        list.forEach(this::fillStatusDesc);
        return list;
    }

    @Override
    public List<MyStudentDocumentVO> getMyStudentDocs(String teacherNo, String docType) {
        List<MyStudentDocumentVO> list = baseMapper.selectMyStudentDocs(teacherNo, docType);
        list.forEach(this::fillMyStudentStatusDesc);
        return list;
    }

    // ==================== 内部方法 ====================

    private void validateDocType(String docType) {
        if (!VALID_DOC_TYPES.contains(docType)) {
            throw new RuntimeException("文档类型不合法，仅支持：任务书/指导书");
        }
    }

    private void validateTitle(String docType, String title) {
        if ("任务书".equals(docType) && (title == null || title.isBlank())) {
            throw new RuntimeException("任务书必须填写题目");
        }
    }

    private void validateStatus(Integer status) {
        if (status == null || (status != 1 && status != 2)) {
            throw new RuntimeException("文档状态不合法，仅支持：1=草稿 / 2=已提交");
        }
    }

    private void fillStatusDesc(DocumentVO vo) {
        if (vo.getStatus() != null) {
            vo.setStatusDesc(vo.getStatus() == 1 ? "草稿" : vo.getStatus() == 2 ? "已提交" : "未知");
        }
        if (vo.getApprovalStatus() != null) {
            vo.setApprovalStatusDesc(ApprovalStatus.fromCode(vo.getApprovalStatus()).getDescription());
        }
    }

    private void fillMyStudentStatusDesc(MyStudentDocumentVO vo) {
        if (vo.getStatus() != null) {
            vo.setStatusDesc(vo.getStatus() == 1 ? "草稿" : vo.getStatus() == 2 ? "已提交" : "未知");
        }
        if (vo.getApprovalStatus() != null) {
            vo.setApprovalStatusDesc(ApprovalStatus.fromCode(vo.getApprovalStatus()).getDescription());
        }
    }
}
