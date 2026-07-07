package com.example.graduationevaluationsystem.controller;

import com.example.graduationevaluationsystem.common.Result;
import com.example.graduationevaluationsystem.dto.StageStatusBatchDTO;
import com.example.graduationevaluationsystem.dto.StageStatusDTO;
import com.example.graduationevaluationsystem.service.StageStatusService;
import com.example.graduationevaluationsystem.vo.StageStatusOverviewVO;
import com.example.graduationevaluationsystem.vo.StageStatusVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 环节状态 Controller
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "环节状态管理", description = "开题、中期、答辩环节的启动与完成")
public class StageStatusController {

    private final StageStatusService stageStatusService;

    // ==================== 8.1 查询学生环节状态 ====================

    @GetMapping("/api/students/{studentNo}/stage-status")
    @Operation(summary = "查询学生环节状态", description = "返回该学生三个环节（开题/中期/答辩）的当前状态")
    public Result<List<StageStatusVO>> getStudentStageStatus(
            @Parameter(description = "学号") @PathVariable String studentNo) {
        return Result.success(stageStatusService.getStudentStageStatus(studentNo));
    }

    // ==================== 8.2 启动环节 ====================

    @PutMapping("/api/stage-status/start")
    @Operation(summary = "启动环节", description = "将指定环节从未开始(1)置为进行中(2)，前置环节必须为已完成(3)")
    public Result<Void> start(@Valid @RequestBody StageStatusDTO dto) {
        stageStatusService.startStage(dto.getStudentNo(), dto.getStage());
        return Result.success();
    }

    // ==================== 8.3 完成环节 ====================

    @PutMapping("/api/stage-status/complete")
    @Operation(summary = "完成环节", description = "将指定环节从进行中(2)置为已完成(3)")
    public Result<Void> complete(@Valid @RequestBody StageStatusDTO dto) {
        stageStatusService.completeStage(dto.getStudentNo(), dto.getStage());
        return Result.success();
    }

    // ==================== 8.4 批量启动环节 ====================

    @PutMapping("/api/stage-status/start-batch")
    @Operation(summary = "批量启动环节", description = "按学生组批量将指定环节从未开始(1)启动为进行中(2)")
    public Result<Void> startBatch(@Valid @RequestBody StageStatusBatchDTO dto) {
        stageStatusService.startBatch(dto.getStage(), dto.getStudentGroupId());
        return Result.success();
    }

    // ==================== 8.5 查询环节进度总览 ====================

    @GetMapping("/api/stage-status/overview")
    @Operation(summary = "查询环节进度总览", description = "返回指定学生组内所有学生在指定环节的状态统计")
    public Result<StageStatusOverviewVO> getOverview(
            @Parameter(description = "环节（开题/中期/答辩）") @RequestParam String stage,
            @Parameter(description = "学生组号") @RequestParam Integer groupId) {
        return Result.success(stageStatusService.getOverview(stage, groupId));
    }
}
