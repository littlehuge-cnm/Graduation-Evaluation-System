package com.example.graduationevaluationsystem.service;

import com.example.graduationevaluationsystem.vo.LoginVO;
import com.example.graduationevaluationsystem.vo.UserInfoVO;

/**
 * 认证 Service
 */
public interface AuthService {

    /**
     * 用户登录
     *
     * @param userType 用户类型（admin/teacher/student）
     * @param username 账号
     * @param password 密码
     * @return 登录信息（含 JWT Token）
     */
    LoginVO login(String userType, String username, String password);

    /**
     * 获取当前登录用户信息
     *
     * @param userType 用户类型
     * @param username 账号
     * @return 用户信息（含身份列表）
     */
    UserInfoVO getUserInfo(String userType, String username);

    /**
     * 修改密码
     *
     * @param userType    用户类型
     * @param username    账号
     * @param oldPassword 原密码
     * @param newPassword 新密码
     */
    void changePassword(String userType, String username, String oldPassword, String newPassword);

    /**
     * 重置用户密码（超管操作）
     *
     * @param userType    用户类型
     * @param username    账号
     * @param newPassword 新密码
     */
    void resetPassword(String userType, String username, String newPassword);

    /**
     * 修改账号状态（超管操作）
     *
     * @param userType      用户类型
     * @param username      账号
     * @param accountStatus 账号状态（1=启用/2=禁用）
     */
    void updateAccountStatus(String userType, String username, Integer accountStatus);
}
