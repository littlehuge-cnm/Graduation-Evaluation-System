package com.example.graduationevaluationsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.graduationevaluationsystem.entity.GroupMapping;
import com.example.graduationevaluationsystem.mapper.GroupMappingMapper;
import com.example.graduationevaluationsystem.service.GroupMappingService;
import org.springframework.stereotype.Service;

/**
 * 环节对应关系 Service 实现
 */
@Service
public class GroupMappingServiceImpl extends ServiceImpl<GroupMappingMapper, GroupMapping> implements GroupMappingService {
}
