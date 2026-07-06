package com.example.graduationevaluationsystem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.graduationevaluationsystem.common.Result;
import com.example.graduationevaluationsystem.entity.TeacherGroup;
import com.example.graduationevaluationsystem.service.TeacherGroupService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 教师分组 Controller
 */
@RestController
@RequestMapping("/api/teacher-groups")
@RequiredArgsConstructor
@Tag(name = "教师分组管理", description = "教师分组的创建、修改、删除及查询")
public class TeacherGroupController {

    private final TeacherGroupService teacherGroupService;

    @PostMapping
    public Result<Void> add(@RequestBody TeacherGroup teacherGroup) {
        teacherGroupService.save(teacherGroup);
        return Result.success();
    }

    @PutMapping("/{groupId}")
    public Result<Void> update(@PathVariable Integer groupId, @RequestBody TeacherGroup teacherGroup) {
        teacherGroup.setGroupId(groupId);
        teacherGroupService.updateById(teacherGroup);
        return Result.success();
    }

    @DeleteMapping("/{groupId}")
    public Result<Void> delete(@PathVariable Integer groupId) {
        teacherGroupService.removeById(groupId);
        return Result.success();
    }

    @GetMapping("/{groupId}")
    public Result<TeacherGroup> getById(@PathVariable Integer groupId) {
        return Result.success(teacherGroupService.getById(groupId));
    }

    @GetMapping
    public Result<List<TeacherGroup>> list() {
        return Result.success(teacherGroupService.list());
    }
}
