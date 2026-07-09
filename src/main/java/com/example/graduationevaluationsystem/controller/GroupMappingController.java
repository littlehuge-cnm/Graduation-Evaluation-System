package com.example.graduationevaluationsystem.controller;

import com.example.graduationevaluationsystem.common.Result;
import com.example.graduationevaluationsystem.dto.GroupMappingBatchAssignDTO;
import com.example.graduationevaluationsystem.dto.GroupMappingDTO;
import com.example.graduationevaluationsystem.service.GroupMappingService;
import com.example.graduationevaluationsystem.vo.GroupMappingVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 环节对应关系 Controller
 */
@RestController
@RequestMapping("/api/group-mappings")
@RequiredArgsConstructor
@Tag(name = "环节对应关系管理", description = "按环节设定教师组与学生组的对应关系")
public class GroupMappingController {

    private final GroupMappingService groupMappingService;

    @PostMapping
    @Operation(summary = "设定环节对应关系", description = "超管按环节（开题/中期/答辩）设定教师组与学生组的对应关系")
    public Result<Void> add(@Valid @RequestBody GroupMappingDTO dto) {
        groupMappingService.createMapping(dto.getStage(), dto.getTeacherGroupId(), dto.getStudentGroupId());
        return Result.success();
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改环节对应关系", description = "超管修改环节对应关系")
    public Result<Void> update(@Parameter(description = "记录编号") @PathVariable Integer id,
            @Valid @RequestBody GroupMappingDTO dto) {
        groupMappingService.updateMapping(id, dto.getStage(), dto.getTeacherGroupId(), dto.getStudentGroupId());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除环节对应关系", description = "超管删除环节对应关系")
    public Result<Void> delete(@Parameter(description = "记录编号") @PathVariable Integer id) {
        groupMappingService.removeById(id);
        return Result.success();
    }

    @GetMapping
    @Operation(summary = "按环节查询对应关系列表", description = "返回对应关系列表（含教师组名、学生组名），可按环节过滤")
    public Result<List<GroupMappingVO>> list(
            @Parameter(description = "环节：开题/中期/答辩") @RequestParam(required = false) String stage) {
        return Result.success(groupMappingService.getListByStage(stage));
    }

    @PostMapping("/random-assign")
    @Operation(summary = "随机分配教师组给学生组", description = "一次性为三个环节（开题/中期/答辩）随机分配教师组给学生组，" +
            "同一环节一对一，同一学生组三环节教师组互不相同，覆盖原有分配。" +
            "如果传入studentGroupIds则仅分配指定学生组，否则分配全部学生组")
    public Result<List<GroupMappingVO>> randomAssign(@RequestBody(required = false) List<Integer> studentGroupIds) {
        if (studentGroupIds == null || studentGroupIds.isEmpty()) {
            return Result.success(groupMappingService.randomAssignAll());
        }
        return Result.success(groupMappingService.randomAssignForGroups(studentGroupIds));
    }

    @PostMapping("/batch-assign")
    @Operation(summary = "批量分配环节对应关系", description = "超管为多个学生组同时指定三个环节的教师组")
    public Result<Void> batchAssign(@Valid @RequestBody List<GroupMappingBatchAssignDTO> list) {
        groupMappingService.batchAssign(list);
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询对应关系详情", description = "按编号查询对应关系详情（含教师组名、学生组名）")
    public Result<GroupMappingVO> getById(@Parameter(description = "记录编号") @PathVariable Integer id) {
        GroupMappingVO vo = groupMappingService.getMappingById(id);
        if (vo == null) {
            return Result.error(404, "环节对应关系不存在");
        }
        return Result.success(vo);
    }
}
