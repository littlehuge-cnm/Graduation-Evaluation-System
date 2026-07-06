package com.example.graduationevaluationsystem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.graduationevaluationsystem.common.Result;
import com.example.graduationevaluationsystem.entity.Admin;
import com.example.graduationevaluationsystem.service.AdminService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 超级管理员 Controller
 */
@RestController
@RequestMapping("/api/admins")
@RequiredArgsConstructor
@Tag(name = "管理员管理", description = "超级管理员的增删改查")
public class AdminController {

    private final AdminService adminService;

    @PostMapping
    public Result<Void> add(@RequestBody Admin admin) {
        adminService.save(admin);
        return Result.success();
    }

    @PutMapping("/{adminId}")
    public Result<Void> update(@PathVariable String adminId, @RequestBody Admin admin) {
        admin.setAdminId(adminId);
        adminService.updateById(admin);
        return Result.success();
    }

    @DeleteMapping("/{adminId}")
    public Result<Void> delete(@PathVariable String adminId) {
        adminService.removeById(adminId);
        return Result.success();
    }

    @GetMapping("/{adminId}")
    public Result<Admin> getById(@PathVariable String adminId) {
        return Result.success(adminService.getById(adminId));
    }

    @GetMapping
    public Result<Page<Admin>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                    @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(adminService.page(new Page<>(pageNum, pageSize)));
    }
}
