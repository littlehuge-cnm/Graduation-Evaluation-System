package com.example.graduationevaluationsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.graduationevaluationsystem.entity.OperationLog;
import com.example.graduationevaluationsystem.mapper.OperationLogMapper;
import com.example.graduationevaluationsystem.service.OperationLogService;
import org.springframework.stereotype.Service;

/**
 * 操作日志 Service 实现
 */
@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {
}
