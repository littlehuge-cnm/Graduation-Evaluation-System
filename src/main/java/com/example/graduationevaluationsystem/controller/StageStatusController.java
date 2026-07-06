package com.example.graduationevaluationsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.graduationevaluationsystem.common.Result;
import com.example.graduationevaluationsystem.entity.StageStatus;
import com.example.graduationevaluationsystem.service.StageStatusService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 环节状态 Controller
 */
@RestController
@RequestMapping("/api/stage-status")
@RequiredArgsConstructor
@Tag(name = "环节状态管理", description = "开题、中期、答辩环节的启动与完成")
public class StageStatusController {

    private final StageStatusService stageStatusService;

    @PutMapping("/start")
    public Result<Void> start(@RequestBody StageStatus stageStatus) {
        LambdaQueryWrapper<StageStatus> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StageStatus::getStudentNo, stageStatus.getStudentNo())
                .eq(StageStatus::getStage, stageStatus.getStage());
        StageStatus update = new StageStatus();
        update.setStatus("进行中");
        update.setStartTime(LocalDateTime.now());
        stageStatusService.update(update, wrapper);
        return Result.success();
    }

    @PutMapping("/complete")
    public Result<Void> complete(@RequestBody StageStatus stageStatus) {
        LambdaQueryWrapper<StageStatus> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StageStatus::getStudentNo, stageStatus.getStudentNo())
                .eq(StageStatus::getStage, stageStatus.getStage());
        StageStatus update = new StageStatus();
        update.setStatus("已完成");
        update.setCompleteTime(LocalDateTime.now());
        stageStatusService.update(update, wrapper);
        return Result.success();
    }

    @GetMapping
    public Result<List<StageStatus>> list(@RequestParam String studentNo) {
        LambdaQueryWrapper<StageStatus> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StageStatus::getStudentNo, studentNo);
        return Result.success(stageStatusService.list(wrapper));
    }
}
