package com.example.graduationevaluationsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.graduationevaluationsystem.entity.TeacherStudent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 师生关系 Mapper
 */
@Mapper
public interface TeacherStudentMapper extends BaseMapper<TeacherStudent> {
}
