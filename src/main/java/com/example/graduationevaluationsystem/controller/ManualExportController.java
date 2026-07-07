package com.example.graduationevaluationsystem.controller;

import com.example.graduationevaluationsystem.common.Result;
import com.example.graduationevaluationsystem.dto.BatchExportDTO;
import com.example.graduationevaluationsystem.dto.GroupExportDTO;
import com.example.graduationevaluationsystem.service.ManualExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 评价手册导出 Controller
 */
@RestController
@RequestMapping("/api/manual-export")
@RequiredArgsConstructor
@Tag(name = "评价手册导出", description = "单个/批量/按组导出 Word 评价手册")
public class ManualExportController {

    private final ManualExportService manualExportService;

    @GetMapping
    @Operation(summary = "导出单个学生评价手册", description = "返回 Word 文件流，文件名为学生姓名.docx")
    public void exportSingle(
            @Parameter(description = "学号") @RequestParam String studentNo,
            HttpServletResponse response) {
        manualExportService.exportSingle(studentNo, response);
    }

    @PostMapping("/batch")
    @Operation(summary = "批量导出评价手册（超管）", description = "返回 ZIP 压缩包，包含多个学生姓名.docx")
    public void exportBatch(@Valid @RequestBody BatchExportDTO dto,
                            HttpServletResponse response) {
        manualExportService.exportBatch(dto.getStudentNos(), response);
    }

    @PostMapping("/by-group")
    @Operation(summary = "按学生组批量导出（超管）", description = "返回 ZIP 压缩包，包含该组全部学生的评价手册")
    public void exportByGroup(@Valid @RequestBody GroupExportDTO dto,
                              HttpServletResponse response) {
        manualExportService.exportByGroup(dto.getStudentGroupId(), response);
    }
}
