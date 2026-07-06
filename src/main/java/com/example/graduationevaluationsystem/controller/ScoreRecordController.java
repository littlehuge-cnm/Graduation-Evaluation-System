package com.example.graduationevaluationsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.graduationevaluationsystem.common.Result;
import com.example.graduationevaluationsystem.entity.ScoreRecord;
import com.example.graduationevaluationsystem.service.ScoreRecordService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评价记录 Controller
 */
@RestController
@RequestMapping("/api/score-records")
@RequiredArgsConstructor
@Tag(name = "评价记录管理", description = "各环节成绩与评语的录入及查询")
public class ScoreRecordController {

    private final ScoreRecordService scoreRecordService;

    @PostMapping
    public Result<Void> add(@RequestBody ScoreRecord scoreRecord) {
        scoreRecord.setRecordTime(LocalDateTime.now());
        scoreRecordService.save(scoreRecord);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Integer id, @RequestBody ScoreRecord scoreRecord) {
        scoreRecord.setId(id);
        scoreRecord.setUpdateTime(LocalDateTime.now());
        scoreRecordService.updateById(scoreRecord);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        scoreRecordService.removeById(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<ScoreRecord> getById(@PathVariable Integer id) {
        return Result.success(scoreRecordService.getById(id));
    }

    @GetMapping
    public Result<List<ScoreRecord>> list(@RequestParam(required = false) String studentNo,
                                          @RequestParam(required = false) String itemType) {
        LambdaQueryWrapper<ScoreRecord> wrapper = new LambdaQueryWrapper<>();
        if (studentNo != null) {
            wrapper.eq(ScoreRecord::getStudentNo, studentNo);
        }
        if (itemType != null) {
            wrapper.eq(ScoreRecord::getItemType, itemType);
        }
        return Result.success(scoreRecordService.list(wrapper));
    }
}
