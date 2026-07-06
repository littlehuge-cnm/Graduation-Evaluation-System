package com.example.graduationevaluationsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.graduationevaluationsystem.entity.TeacherGroup;
import com.example.graduationevaluationsystem.mapper.TeacherGroupMapper;
import com.example.graduationevaluationsystem.service.TeacherGroupService;
import org.springframework.stereotype.Service;

/**
 * 教师分组 Service 实现
 */
@Service
public class TeacherGroupServiceImpl extends ServiceImpl<TeacherGroupMapper, TeacherGroup> implements TeacherGroupService {
}
