package com.example.graduationevaluationsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.graduationevaluationsystem.entity.Admin;
import com.example.graduationevaluationsystem.mapper.AdminMapper;
import com.example.graduationevaluationsystem.service.AdminService;
import org.springframework.stereotype.Service;

/**
 * 超级管理员 Service 实现
 */
@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminService {
}
