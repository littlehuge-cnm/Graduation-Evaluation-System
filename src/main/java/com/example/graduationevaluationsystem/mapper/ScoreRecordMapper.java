package com.example.graduationevaluationsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.graduationevaluationsystem.entity.ScoreRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评价记录 Mapper
 */
@Mapper
public interface ScoreRecordMapper extends BaseMapper<ScoreRecord> {
}
