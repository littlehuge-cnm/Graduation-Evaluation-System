package com.example.graduationevaluationsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.graduationevaluationsystem.entity.Admin;
import org.apache.ibatis.annotations.Mapper;

/**
 * 超级管理员 Mapper
 */
@Mapper
public interface AdminMapper extends BaseMapper<Admin> {
}
