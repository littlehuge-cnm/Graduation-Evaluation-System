package com.example.graduationevaluationsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.graduationevaluationsystem.common.Result;
import com.example.graduationevaluationsystem.dto.GroupStatusDTO;
import com.example.graduationevaluationsystem.dto.TeacherGroupDTO;
import com.example.graduationevaluationsystem.entity.TeacherGroup;
import com.example.graduationevaluationsystem.service.TeacherGroupService;
import com.example.graduationevaluationsystem.vo.TeacherGroupVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 教师分组 Controller
 */
@RestController
@RequestMapping("/api/teacher-groups")
@RequiredArgsConstructor
@Tag(name = "教师分组管理", description = "教师分组的创建、修改、删除、查询及状态管理")
public class TeacherGroupController {

    private final TeacherGroupService teacherGroupService;

    @PostMapping
    @Operation(summary = "创建教师分组", description = "超管创建教师分组，每组3人（组长/秘书/普通成员）")
    public Result<Void> add(@Valid @RequestBody TeacherGroupDTO dto) {
        // 组内判重：组长、秘书、普通成员工号不能重复
        if (dto.getLeaderNo().equals(dto.getSecretaryNo())
                || dto.getLeaderNo().equals(dto.getMemberNo())
                || dto.getSecretaryNo().equals(dto.getMemberNo())) {
            return Result.error(400, "组内教师工号不能重复");
        }
        // 跨组判重：检查教师是否已在其他分组中
        String[] teacherNos = {dto.getLeaderNo(), dto.getSecretaryNo(), dto.getMemberNo()};
        for (String teacherNo : teacherNos) {
            long count = teacherGroupService.count(new LambdaQueryWrapper<TeacherGroup>()
                    .and(w -> w.eq(TeacherGroup::getLeaderNo, teacherNo)
                            .or().eq(TeacherGroup::getSecretaryNo, teacherNo)
                            .or().eq(TeacherGroup::getMemberNo, teacherNo)));
            if (count > 0) {
                return Result.error(400, "教师工号 " + teacherNo + " 已在其他分组中，不能重复分组");
            }
        }
        TeacherGroup group = new TeacherGroup();
        BeanUtils.copyProperties(dto, group);
        if (group.getGroupStatus() == null) {
            group.setGroupStatus(1);
        }
        teacherGroupService.save(group);
        return Result.success();
    }

    @PutMapping("/{groupId}")
    @Operation(summary = "修改教师分组", description = "超管修改教师分组信息")
    public Result<Void> update(@Parameter(description = "分组编号") @PathVariable Integer groupId,
                               @RequestBody TeacherGroupDTO dto) {
        TeacherGroup existGroup = teacherGroupService.getById(groupId);
        if (existGroup == null) {
            return Result.error(404, "教师分组不存在");
        }
        if (dto.getLeaderNo().equals(dto.getSecretaryNo())
                || dto.getLeaderNo().equals(dto.getMemberNo())
                || dto.getSecretaryNo().equals(dto.getMemberNo())) {
            return Result.error(400, "组内教师工号不能重复");
        }
        String[] teacherNos = {dto.getLeaderNo(), dto.getSecretaryNo(), dto.getMemberNo()};
        for (String teacherNo : teacherNos) {
            long count = teacherGroupService.count(new LambdaQueryWrapper<TeacherGroup>()
                    .ne(TeacherGroup::getGroupId, groupId)
                    .and(w -> w.eq(TeacherGroup::getLeaderNo, teacherNo)
                            .or().eq(TeacherGroup::getSecretaryNo, teacherNo)
                            .or().eq(TeacherGroup::getMemberNo, teacherNo)));
            if (count > 0) {
                return Result.error(400, "教师工号 " + teacherNo + " 已在其他分组中，不能重复分组");
            }
        }
        TeacherGroup group = new TeacherGroup();
        BeanUtils.copyProperties(dto, group);
        group.setGroupId(groupId);
        teacherGroupService.updateById(group);
        return Result.success();
    }

    @DeleteMapping("/{groupId}")
    @Operation(summary = "删除教师分组", description = "超管删除教师分组")
    public Result<Void> delete(@Parameter(description = "分组编号") @PathVariable Integer groupId) {
        teacherGroupService.removeById(groupId);
        return Result.success();
    }

    @GetMapping("/{groupId}")
    @Operation(summary = "查询教师分组详情", description = "返回分组信息及成员姓名")
    public Result<TeacherGroupVO> getById(@Parameter(description = "分组编号") @PathVariable Integer groupId) {
        TeacherGroupVO vo = teacherGroupService.getGroupById(groupId);
        if (vo == null) {
            return Result.error(404, "教师分组不存在");
        }
        return Result.success(vo);
    }

    @GetMapping
    @Operation(summary = "查询教师分组列表", description = "返回全部分组列表，含成员姓名与状态描述")
    public Result<List<TeacherGroupVO>> list() {
        return Result.success(teacherGroupService.getAllGroups());
    }

    @PutMapping("/{groupId}/status")
    @Operation(summary = "修改教师分组状态", description = "超管启用/停用教师分组，状态流转：1待启用→2已启用→3已停用")
    public Result<Void> updateStatus(
            @Parameter(description = "分组编号") @PathVariable Integer groupId,
            @Valid @RequestBody GroupStatusDTO dto) {
        teacherGroupService.updateGroupStatus(groupId, dto.getGroupStatus());
        return Result.success();
    }
}
