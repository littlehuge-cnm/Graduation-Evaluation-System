package com.example.graduationevaluationsystem.controller;

import com.example.graduationevaluationsystem.common.Result;
import com.example.graduationevaluationsystem.dto.AccountStatusDTO;
import com.example.graduationevaluationsystem.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 账号状态管理 Controller
 */
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(name = "账号状态管理", description = "超管启用/禁用任意账号")
public class AccountController {

    private final AuthService authService;

    @PutMapping("/status")
    @Operation(summary = "修改账号状态", description = "超管修改任意用户账号状态（1=启用/2=禁用），禁用后无法登录但数据保留")
    public Result<Void> updateStatus(@Valid @RequestBody AccountStatusDTO dto) {
        authService.updateAccountStatus(dto.getUserType(), dto.getUsername(), dto.getAccountStatus());
        return Result.success("账号状态修改成功", null);
    }
}
