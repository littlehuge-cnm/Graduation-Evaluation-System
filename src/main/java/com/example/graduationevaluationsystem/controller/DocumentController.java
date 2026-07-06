package com.example.graduationevaluationsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.graduationevaluationsystem.common.Result;
import com.example.graduationevaluationsystem.entity.Document;
import com.example.graduationevaluationsystem.service.DocumentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档 Controller
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "文档管理", description = "任务书与指导书的保存、提交及查询")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping
    public Result<Void> add(@RequestBody Document document) {
        document.setUpdateTime(LocalDateTime.now());
        documentService.save(document);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Integer id, @RequestBody Document document) {
        document.setId(id);
        document.setUpdateTime(LocalDateTime.now());
        documentService.updateById(document);
        return Result.success();
    }

    @PutMapping("/{id}/submit")
    public Result<Void> submit(@PathVariable Integer id) {
        Document document = new Document();
        document.setId(id);
        document.setStatus("已提交");
        document.setSubmitTime(LocalDateTime.now());
        document.setUpdateTime(LocalDateTime.now());
        documentService.updateById(document);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        documentService.removeById(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<Document> getById(@PathVariable Integer id) {
        return Result.success(documentService.getById(id));
    }

    @GetMapping
    public Result<List<Document>> list(@RequestParam(required = false) String studentNo,
                                       @RequestParam(required = false) String docType) {
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        if (studentNo != null) {
            wrapper.eq(Document::getStudentNo, studentNo);
        }
        if (docType != null) {
            wrapper.eq(Document::getDocType, docType);
        }
        return Result.success(documentService.list(wrapper));
    }
}
