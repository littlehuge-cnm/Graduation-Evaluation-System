package com.example.graduationevaluationsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.graduationevaluationsystem.entity.TeacherStudent;
import com.example.graduationevaluationsystem.mapper.TeacherStudentMapper;
import com.example.graduationevaluationsystem.service.TeacherStudentService;
import org.springframework.stereotype.Service;

/**
 * 师生关系 Service 实现
 */
@Service
public class TeacherStudentServiceImpl extends ServiceImpl<TeacherStudentMapper, TeacherStudent> implements TeacherStudentService {
}
