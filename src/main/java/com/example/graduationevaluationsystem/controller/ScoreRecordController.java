package com.example.graduationevaluationsystem.controller;

import com.example.graduationevaluationsystem.common.Result;
import com.example.graduationevaluationsystem.dto.ScoreRecordDTO;
import com.example.graduationevaluationsystem.service.ScoreRecordService;
import com.example.graduationevaluationsystem.vo.ScoreRecordTodoVO;
import com.example.graduationevaluationsystem.vo.ScoreRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评价记录 Controller
 */
@RestController
@RequestMapping("/api/score-records")
@RequiredArgsConstructor
@Tag(name = "评价记录管理", description = "各环节成绩与评语的录入、修改、确认、解锁及查询")
public class ScoreRecordController {

    private final ScoreRecordService scoreRecordService;

    @PostMapping
    @Operation(summary = "录入评价记录",
            description = "根据 itemType 校验分项成绩格式，未传 score 时按分项求和。" +
                    "recorderNo 通过请求参数传入（后续接入 JWT 后改为自动获取）")
    public Result<Void> add(@Valid @RequestBody ScoreRecordDTO dto,
                            @Parameter(description = "录入人账号（工号/管理员ID）")
                            @RequestParam String recorderNo) {
        scoreRecordService.createRecord(dto.getStudentNo(), dto.getItemType(),
                dto.getSubScores(), dto.getScore(), dto.getGrade(),
                dto.getComment(), dto.getDefenseRecord(), dto.getRecordStatus(), recorderNo);
        return Result.success();
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改评价记录", description = "仅暂存状态可修改，已确认需先解锁。支持部分字段更新")
    public Result<Void> update(@Parameter(description = "记录编号") @PathVariable Integer id,
                               @RequestBody ScoreRecordDTO dto) {
        scoreRecordService.updateRecord(id, dto.getSubScores(), dto.getScore(),
                dto.getGrade(), dto.getComment(), dto.getDefenseRecord(), dto.getRecordStatus());
        return Result.success();
    }

    @PutMapping("/{id}/confirm")
    @Operation(summary = "确认评价记录", description = "录入人将暂存记录确认锁定，不可再改")
    public Result<Void> confirm(@Parameter(description = "记录编号") @PathVariable Integer id) {
        scoreRecordService.confirmRecord(id);
        return Result.success();
    }

    @PutMapping("/{id}/unlock")
    @Operation(summary = "解锁评价记录", description = "超管将已确认记录解锁退回暂存，允许修改")
    public Result<Void> unlock(@Parameter(description = "记录编号") @PathVariable Integer id) {
        scoreRecordService.unlockRecord(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询评价记录详情", description = "含录入人姓名、状态描述")
    public Result<ScoreRecordVO> getById(@Parameter(description = "记录编号") @PathVariable Integer id) {
        ScoreRecordVO vo = scoreRecordService.getRecordById(id);
        if (vo == null) {
            return Result.error(404, "评价记录不存在");
        }
        return Result.success(vo);
    }

    @GetMapping("/todo")
    @Operation(summary = "查询待录入列表（按角色）",
            description = "教师调用时按其身份（组长/秘书/指导教师/评阅教师）返回对应待录入项；" +
                    "管理员调用时返回待委员会评定的学生列表")
    public Result<List<ScoreRecordTodoVO>> todo(
            @Parameter(description = "录入人账号（工号/管理员ID）") @RequestParam String recorderNo,
            @Parameter(description = "用户类型（teacher/admin）") @RequestParam String userType) {
        return Result.success(scoreRecordService.getTodoList(recorderNo, userType));
    }
}
