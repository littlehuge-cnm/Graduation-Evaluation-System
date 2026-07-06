package com.example.graduationevaluationsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.graduationevaluationsystem.common.Result;
import com.example.graduationevaluationsystem.entity.OperationLog;
import com.example.graduationevaluationsystem.service.OperationLogService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 操作日志 Controller
 */
@RestController
@RequestMapping("/api/operation-logs")
@RequiredArgsConstructor
@Tag(name = "操作日志", description = "操作日志的分页查询与详情")
public class OperationLogController {

    private final OperationLogService operationLogService;

    @GetMapping("/{id}")
    public Result<OperationLog> getById(@PathVariable Long id) {
        return Result.success(operationLogService.getById(id));
    }

    @GetMapping
    public Result<Page<OperationLog>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                           @RequestParam(defaultValue = "10") Integer pageSize,
                                           @RequestParam(required = false) String userType,
                                           @RequestParam(required = false) String userNo,
                                           @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startTime,
                                           @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endTime) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(userType)) {
            wrapper.eq(OperationLog::getUserType, userType);
        }
        if (StringUtils.hasText(userNo)) {
            wrapper.eq(OperationLog::getUserNo, userNo);
        }
        if (startTime != null) {
            wrapper.ge(OperationLog::getOperationTime, startTime.atStartOfDay());
        }
        if (endTime != null) {
            wrapper.le(OperationLog::getOperationTime, endTime.plusDays(1).atStartOfDay());
        }
        wrapper.orderByDesc(OperationLog::getOperationTime);
        return Result.success(operationLogService.page(new Page<>(pageNum, pageSize), wrapper));
    }
}
