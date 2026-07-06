package com.example.graduationevaluationsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.graduationevaluationsystem.entity.StageStatus;
import com.example.graduationevaluationsystem.mapper.StageStatusMapper;
import com.example.graduationevaluationsystem.service.StageStatusService;
import org.springframework.stereotype.Service;

/**
 * 环节状态 Service 实现
 */
@Service
public class StageStatusServiceImpl extends ServiceImpl<StageStatusMapper, StageStatus> implements StageStatusService {
}
