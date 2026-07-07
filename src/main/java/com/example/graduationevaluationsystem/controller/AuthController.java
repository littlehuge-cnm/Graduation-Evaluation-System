package com.example.graduationevaluationsystem.controller;

import cn.hutool.jwt.JWTPayload;
import com.example.graduationevaluationsystem.common.JwtUtils;
import com.example.graduationevaluationsystem.common.Result;
import com.example.graduationevaluationsystem.dto.LoginDTO;
import com.example.graduationevaluationsystem.dto.PasswordChangeDTO;
import com.example.graduationevaluationsystem.dto.PasswordResetDTO;
import com.example.graduationevaluationsystem.service.AuthService;
import com.example.graduationevaluationsystem.vo.LoginVO;
import com.example.graduationevaluationsystem.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证 Controller
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证模块", description = "登录、退出、获取用户信息、修改密码、重置密码、账号状态管理")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "根据用户类型验证账号密码，返回 JWT Token")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        LoginVO vo = authService.login(dto.getUserType(), dto.getUsername(), dto.getPassword());
        return Result.success("登录成功", vo);
    }

    @PostMapping("/logout")
    @Operation(summary = "用户退出", description = "客户端销毁 Token 即可，服务端无状态")
    public Result<Void> logout() {
        return Result.success("退出成功", null);
    }

    @GetMapping("/info")
    @Operation(summary = "获取当前登录用户信息", description = "从 JWT Token 中解析用户信息，教师用户返回身份列表")
    public Result<UserInfoVO> info(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            return Result.error(401, "未认证");
        }
        JWTPayload payload = JwtUtils.verify(token);
        if (payload == null) {
            return Result.error(401, "Token 无效或已过期");
        }
        String userType = (String) payload.getClaim("userType");
        String username = (String) payload.getClaim("username");
        UserInfoVO vo = authService.getUserInfo(userType, username);
        return Result.success(vo);
    }

    @PutMapping("/password")
    @Operation(summary = "修改密码", description = "用户修改自己的密码，需验证原密码")
    public Result<Void> changePassword(HttpServletRequest request,
                                       @Valid @RequestBody PasswordChangeDTO dto) {
        String token = extractToken(request);
        if (token == null) {
            return Result.error(401, "未认证");
        }
        JWTPayload payload = JwtUtils.verify(token);
        if (payload == null) {
            return Result.error(401, "Token 无效或已过期");
        }
        String userType = (String) payload.getClaim("userType");
        String username = (String) payload.getClaim("username");
        authService.changePassword(userType, username, dto.getOldPassword(), dto.getNewPassword());
        return Result.success("密码修改成功", null);
    }

    @PutMapping("/password/reset")
    @Operation(summary = "重置用户密码", description = "超管重置任意用户密码，无需原密码")
    public Result<Void> resetPassword(@Valid @RequestBody PasswordResetDTO dto) {
        authService.resetPassword(dto.getUserType(), dto.getUsername(), dto.getNewPassword());
        return Result.success("密码重置成功", null);
    }

    /**
     * 从请求头中提取 JWT Token
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
