package com.example.graduationevaluationsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.graduationevaluationsystem.entity.GroupMapping;
import org.apache.ibatis.annotations.Mapper;

/**
 * 环节对应关系 Mapper
 */
@Mapper
public interface GroupMappingMapper extends BaseMapper<GroupMapping> {
}
