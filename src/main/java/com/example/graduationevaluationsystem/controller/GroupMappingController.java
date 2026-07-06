package com.example.graduationevaluationsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.graduationevaluationsystem.common.Result;
import com.example.graduationevaluationsystem.entity.GroupMapping;
import com.example.graduationevaluationsystem.service.GroupMappingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 环节对应关系 Controller
 */
@RestController
@RequestMapping("/api/group-mappings")
@RequiredArgsConstructor
@Tag(name = "环节对应关系", description = "按环节设定教师组与学生组的对应关系")
public class GroupMappingController {

    private final GroupMappingService groupMappingService;

    @PostMapping
    public Result<Void> add(@RequestBody GroupMapping groupMapping) {
        groupMappingService.save(groupMapping);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Integer id, @RequestBody GroupMapping groupMapping) {
        groupMapping.setId(id);
        groupMappingService.updateById(groupMapping);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        groupMappingService.removeById(id);
        return Result.success();
    }

    @GetMapping
    public Result<List<GroupMapping>> list(@RequestParam(required = false) String stage) {
        LambdaQueryWrapper<GroupMapping> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(stage)) {
            wrapper.eq(GroupMapping::getStage, stage);
        }
        return Result.success(groupMappingService.list(wrapper));
    }
}
