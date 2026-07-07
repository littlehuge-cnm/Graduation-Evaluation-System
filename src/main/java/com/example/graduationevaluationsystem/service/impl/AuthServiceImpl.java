package com.example.graduationevaluationsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.graduationevaluationsystem.common.JwtUtils;
import com.example.graduationevaluationsystem.common.enums.AccountStatus;
import com.example.graduationevaluationsystem.entity.Admin;
import com.example.graduationevaluationsystem.entity.Student;
import com.example.graduationevaluationsystem.entity.Teacher;
import com.example.graduationevaluationsystem.entity.TeacherGroup;
import com.example.graduationevaluationsystem.entity.TeacherStudent;
import com.example.graduationevaluationsystem.service.AdminService;
import com.example.graduationevaluationsystem.service.AuthService;
import com.example.graduationevaluationsystem.service.StudentService;
import com.example.graduationevaluationsystem.service.TeacherService;
import com.example.graduationevaluationsystem.service.TeacherGroupService;
import com.example.graduationevaluationsystem.service.TeacherStudentService;
import com.example.graduationevaluationsystem.vo.LoginVO;
import com.example.graduationevaluationsystem.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 认证 Service 实现
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AdminService adminService;
    private final TeacherService teacherService;
    private final StudentService studentService;
    private final TeacherGroupService teacherGroupService;
    private final TeacherStudentService teacherStudentService;

    @Override
    public LoginVO login(String userType, String username, String password) {
        String name;
        Integer accountStatus;

        switch (userType) {
            case "admin" -> {
                Admin admin = adminService.getById(username);
                if (admin == null) {
                    throw new RuntimeException("管理员账号不存在");
                }
                if (!password.equals(admin.getPassword())) {
                    throw new RuntimeException("密码错误");
                }
                accountStatus = admin.getAccountStatus();
                name = admin.getAdminName();
            }
            case "teacher" -> {
                Teacher teacher = teacherService.getById(username);
                if (teacher == null) {
                    throw new RuntimeException("教师工号不存在");
                }
                if (!password.equals(teacher.getPassword())) {
                    throw new RuntimeException("密码错误");
                }
                accountStatus = teacher.getAccountStatus();
                name = teacher.getTeacherName();
            }
            case "student" -> {
                Student student = studentService.getById(username);
                if (student == null) {
                    throw new RuntimeException("学号不存在");
                }
                if (!password.equals(student.getPassword())) {
                    throw new RuntimeException("密码错误");
                }
                accountStatus = student.getAccountStatus();
                name = student.getStudentName();
            }
            default -> throw new RuntimeException("无效的用户类型：" + userType);
        }

        if (accountStatus != null && accountStatus == AccountStatus.DISABLED.getCode()) {
            throw new RuntimeException("账号已被禁用，请联系管理员");
        }

        String token = JwtUtils.generateToken(userType, username, name);

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserType(userType);
        vo.setUsername(username);
        vo.setName(name);
        return vo;
    }

    @Override
    public UserInfoVO getUserInfo(String userType, String username) {
        UserInfoVO vo = new UserInfoVO();
        vo.setUserType(userType);
        vo.setUsername(username);
        vo.setIdentities(new ArrayList<>());

        switch (userType) {
            case "admin" -> {
                Admin admin = adminService.getById(username);
                if (admin != null) {
                    vo.setName(admin.getAdminName());
                }
            }
            case "teacher" -> {
                Teacher teacher = teacherService.getById(username);
                if (teacher == null) {
                    throw new RuntimeException("教师不存在");
                }
                vo.setName(teacher.getTeacherName());

                List<String> identities = new ArrayList<>();

                // 查询是否为指导教师或评阅教师
                LambdaQueryWrapper<TeacherStudent> tsWrapper = new LambdaQueryWrapper<>();
                tsWrapper.eq(TeacherStudent::getTeacherNo, username)
                         .eq(TeacherStudent::getRelationStatus, 1);
                List<TeacherStudent> tsList = teacherStudentService.list(tsWrapper);
                for (TeacherStudent ts : tsList) {
                    if ("指导".equals(ts.getRelationType()) && !identities.contains("指导教师")) {
                        identities.add("指导教师");
                    }
                    if ("评阅".equals(ts.getRelationType()) && !identities.contains("评阅教师")) {
                        identities.add("评阅教师");
                    }
                }

                // 查询是否为组长或秘书
                LambdaQueryWrapper<TeacherGroup> tgWrapper = new LambdaQueryWrapper<>();
                tgWrapper.eq(TeacherGroup::getLeaderNo, username)
                         .or()
                         .eq(TeacherGroup::getSecretaryNo, username)
                         .or()
                         .eq(TeacherGroup::getMemberNo, username);
                List<TeacherGroup> tgList = teacherGroupService.list(tgWrapper);
                for (TeacherGroup tg : tgList) {
                    if (username.equals(tg.getLeaderNo()) && !identities.contains("组长")) {
                        identities.add("组长");
                    }
                    if (username.equals(tg.getSecretaryNo()) && !identities.contains("秘书")) {
                        identities.add("秘书");
                    }
                    if (username.equals(tg.getMemberNo()) && !identities.contains("普通成员")) {
                        identities.add("普通成员");
                    }
                }

                vo.setIdentities(identities);
            }
            case "student" -> {
                Student student = studentService.getById(username);
                if (student != null) {
                    vo.setName(student.getStudentName());
                }
            }
            default -> {}
        }

        return vo;
    }

    @Override
    public void changePassword(String userType, String username, String oldPassword, String newPassword) {
        switch (userType) {
            case "admin" -> {
                Admin admin = adminService.getById(username);
                if (admin == null) {
                    throw new RuntimeException("管理员账号不存在");
                }
                if (!oldPassword.equals(admin.getPassword())) {
                    throw new RuntimeException("原密码错误");
                }
                admin.setPassword(newPassword);
                adminService.updateById(admin);
            }
            case "teacher" -> {
                Teacher teacher = teacherService.getById(username);
                if (teacher == null) {
                    throw new RuntimeException("教师工号不存在");
                }
                if (!oldPassword.equals(teacher.getPassword())) {
                    throw new RuntimeException("原密码错误");
                }
                teacher.setPassword(newPassword);
                teacherService.updateById(teacher);
            }
            case "student" -> {
                Student student = studentService.getById(username);
                if (student == null) {
                    throw new RuntimeException("学号不存在");
                }
                if (!oldPassword.equals(student.getPassword())) {
                    throw new RuntimeException("原密码错误");
                }
                student.setPassword(newPassword);
                studentService.updateById(student);
            }
            default -> throw new RuntimeException("无效的用户类型：" + userType);
        }
    }

    @Override
    public void resetPassword(String userType, String username, String newPassword) {
        switch (userType) {
            case "admin" -> {
                Admin admin = adminService.getById(username);
                if (admin == null) {
                    throw new RuntimeException("管理员账号不存在");
                }
                admin.setPassword(newPassword);
                adminService.updateById(admin);
            }
            case "teacher" -> {
                Teacher teacher = teacherService.getById(username);
                if (teacher == null) {
                    throw new RuntimeException("教师工号不存在");
                }
                teacher.setPassword(newPassword);
                teacherService.updateById(teacher);
            }
            case "student" -> {
                Student student = studentService.getById(username);
                if (student == null) {
                    throw new RuntimeException("学号不存在");
                }
                student.setPassword(newPassword);
                studentService.updateById(student);
            }
            default -> throw new RuntimeException("无效的用户类型：" + userType);
        }
    }

    @Override
    public void updateAccountStatus(String userType, String username, Integer accountStatus) {
        AccountStatus.fromCode(accountStatus);

        switch (userType) {
            case "admin" -> {
                Admin admin = adminService.getById(username);
                if (admin == null) {
                    throw new RuntimeException("管理员账号不存在");
                }
                admin.setAccountStatus(accountStatus);
                adminService.updateById(admin);
            }
            case "teacher" -> {
                Teacher teacher = teacherService.getById(username);
                if (teacher == null) {
                    throw new RuntimeException("教师工号不存在");
                }
                teacher.setAccountStatus(accountStatus);
                teacherService.updateById(teacher);
            }
            case "student" -> {
                Student student = studentService.getById(username);
                if (student == null) {
                    throw new RuntimeException("学号不存在");
                }
                student.setAccountStatus(accountStatus);
                studentService.updateById(student);
            }
            default -> throw new RuntimeException("无效的用户类型：" + userType);
        }
    }
}
