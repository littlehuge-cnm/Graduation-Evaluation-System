package com.example.graduationevaluationsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.graduationevaluationsystem.entity.Student;
import com.example.graduationevaluationsystem.mapper.StudentMapper;
import com.example.graduationevaluationsystem.service.StudentService;
import org.springframework.stereotype.Service;

/**
 * 学生 Service 实现
 */
@Service
public class StudentServiceImpl extends ServiceImpl<StudentMapper, Student> implements StudentService {
}
