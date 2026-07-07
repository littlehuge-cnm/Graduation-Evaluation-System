package com.example.graduationevaluationsystem.controller;

import com.example.graduationevaluationsystem.common.Result;
import com.example.graduationevaluationsystem.dto.StudentGroupDTO;
import com.example.graduationevaluationsystem.service.StudentGroupService;
import com.example.graduationevaluationsystem.vo.StudentGroupVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学生分组 Controller
 */
@RestController
@RequestMapping("/api/student-groups")
@RequiredArgsConstructor
@Tag(name = "学生分组管理", description = "学生分组的创建、修改、删除、查询")
public class StudentGroupController {

    private final StudentGroupService studentGroupService;

    @PostMapping
    @Operation(summary = "创建学生分组", description = "超管创建学生分组，可同时指定组内学生")
    public Result<Void> add(@RequestBody StudentGroupDTO dto) {
        studentGroupService.createGroup(dto.getGroupName(), dto.getStudentNos());
        return Result.success();
    }

    @PutMapping("/{groupId}")
    @Operation(summary = "修改学生分组", description = "超管修改学生分组信息，可重新绑定组内学生")
    public Result<Void> update(@Parameter(description = "分组编号") @PathVariable Integer groupId,
                               @RequestBody StudentGroupDTO dto) {
        studentGroupService.updateGroup(groupId, dto.getGroupName(), dto.getStudentNos());
        return Result.success();
    }

    @DeleteMapping("/{groupId}")
    @Operation(summary = "删除学生分组", description = "超管删除学生分组，组内学生的分组绑定将被解除")
    public Result<Void> delete(@Parameter(description = "分组编号") @PathVariable Integer groupId) {
        studentGroupService.deleteGroup(groupId);
        return Result.success();
    }

    @GetMapping
    @Operation(summary = "查询学生分组列表", description = "返回全部分组列表，含每组学生数量")
    public Result<List<StudentGroupVO>> list() {
        return Result.success(studentGroupService.getAllGroups());
    }

    @GetMapping("/{groupId}")
    @Operation(summary = "查询学生分组详情", description = "返回分组信息及组内学生列表")
    public Result<StudentGroupVO> getById(@Parameter(description = "分组编号") @PathVariable Integer groupId) {
        StudentGroupVO vo = studentGroupService.getGroupById(groupId);
        if (vo == null) {
            return Result.error(404, "学生分组不存在");
        }
        return Result.success(vo);
    }
}
