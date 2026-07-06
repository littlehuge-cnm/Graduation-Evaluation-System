package com.example.graduationevaluationsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.graduationevaluationsystem.entity.ScoreRecord;
import com.example.graduationevaluationsystem.mapper.ScoreRecordMapper;
import com.example.graduationevaluationsystem.service.ScoreRecordService;
import org.springframework.stereotype.Service;

/**
 * 评价记录 Service 实现
 */
@Service
public class ScoreRecordServiceImpl extends ServiceImpl<ScoreRecordMapper, ScoreRecord> implements ScoreRecordService {
}
