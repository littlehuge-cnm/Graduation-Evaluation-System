package com.example.graduationevaluationsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.graduationevaluationsystem.entity.Student;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学生 Mapper
 */
@Mapper
public interface StudentMapper extends BaseMapper<Student> {
}
