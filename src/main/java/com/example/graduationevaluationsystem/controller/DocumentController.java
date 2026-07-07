package com.example.graduationevaluationsystem.controller;

import com.example.graduationevaluationsystem.common.Result;
import com.example.graduationevaluationsystem.dto.ApprovalDTO;
import com.example.graduationevaluationsystem.dto.DocumentDTO;
import com.example.graduationevaluationsystem.service.DocumentService;
import com.example.graduationevaluationsystem.vo.DocumentVO;
import com.example.graduationevaluationsystem.vo.MyStudentDocumentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文档 Controller
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "文档管理", description = "任务书与指导书的保存、提交、退回、审批及查询")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping
    @Operation(summary = "保存/更新文档", description = "指导教师保存或更新任务书/指导书，支持草稿保存")
    public Result<Void> add(@Valid @RequestBody DocumentDTO dto) {
        documentService.saveDocument(dto.getStudentNo(), dto.getDocType(), dto.getTitle(),
                dto.getSubjectCategory(), dto.getSubjectType(), dto.getSubjectNewOld(),
                dto.getContent(), dto.getStatus());
        return Result.success();
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改文档", description = "指导教师修改文档，仅草稿状态可修改")
    public Result<Void> update(@Parameter(description = "文档编号") @PathVariable Integer id,
                               @Valid @RequestBody DocumentDTO dto) {
        documentService.updateDocument(id, dto.getStudentNo(), dto.getDocType(), dto.getTitle(),
                dto.getSubjectCategory(), dto.getSubjectType(), dto.getSubjectNewOld(),
                dto.getContent(), dto.getStatus());
        return Result.success();
    }

    @PutMapping("/{id}/submit")
    @Operation(summary = "提交文档", description = "指导教师将文档从草稿提交为已提交，任务书自动进入待系审")
    public Result<Void> submit(@Parameter(description = "文档编号") @PathVariable Integer id) {
        documentService.submitDocument(id);
        return Result.success();
    }

    @PutMapping("/{id}/rollback")
    @Operation(summary = "退回文档", description = "超管将已提交文档退回为草稿，清空审批状态")
    public Result<Void> rollback(@Parameter(description = "文档编号") @PathVariable Integer id) {
        documentService.rollbackDocument(id);
        return Result.success();
    }

    @PutMapping("/{id}/dept-approval")
    @Operation(summary = "系主任审核", description = "超管代行系主任审核：2=系通过（自动转为待院审）/3=系驳回")
    public Result<Void> deptApproval(@Parameter(description = "文档编号") @PathVariable Integer id,
                                     @Valid @RequestBody ApprovalDTO dto) {
        documentService.deptApproval(id, dto.getApprovalStatus());
        return Result.success();
    }

    @PutMapping("/{id}/college-approval")
    @Operation(summary = "院长审核", description = "超管代行院长审核：5=院通过（终态）/6=院驳回")
    public Result<Void> collegeApproval(@Parameter(description = "文档编号") @PathVariable Integer id,
                                        @Valid @RequestBody ApprovalDTO dto) {
        documentService.collegeApproval(id, dto.getApprovalStatus());
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询文档详情", description = "按编号查询文档详情，含学生姓名及状态描述")
    public Result<DocumentVO> getById(@Parameter(description = "文档编号") @PathVariable Integer id) {
        DocumentVO vo = documentService.getDocById(id);
        if (vo == null) {
            return Result.error(404, "文档不存在");
        }
        return Result.success(vo);
    }

    @GetMapping("/my-students")
    @Operation(summary = "查询指导教师待填写文档列表", description = "返回当前教师所指导学生的文档填写情况")
    public Result<List<MyStudentDocumentVO>> myStudents(
            @Parameter(description = "教师工号") @RequestParam String teacherNo,
            @Parameter(description = "文档类型：任务书/指导书") @RequestParam(required = false) String docType) {
        return Result.success(documentService.getMyStudentDocs(teacherNo, docType));
    }
}
