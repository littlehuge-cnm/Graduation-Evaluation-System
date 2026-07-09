package com.example.graduationevaluationsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.graduationevaluationsystem.common.enums.StageStatusType;
import com.example.graduationevaluationsystem.entity.StageStatus;
import com.example.graduationevaluationsystem.entity.Student;
import com.example.graduationevaluationsystem.mapper.StageStatusMapper;
import com.example.graduationevaluationsystem.mapper.StudentMapper;
import com.example.graduationevaluationsystem.service.StageStatusService;
import com.example.graduationevaluationsystem.vo.StageStatusOverviewVO;
import com.example.graduationevaluationsystem.vo.StageStatusVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 环节状态 Service 实现
 */
@Service
@RequiredArgsConstructor
public class StageStatusServiceImpl extends ServiceImpl<StageStatusMapper, StageStatus> implements StageStatusService {

    /**
     * 三个环节的固定顺序
     */
    private static final List<String> STAGES = List.of("开题", "中期", "答辩");

    private final StudentMapper studentMapper;

    @Override
    public List<StageStatusVO> getStudentStageStatus(String studentNo) {
        // 查询该学生的所有环节状态记录
        LambdaQueryWrapper<StageStatus> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StageStatus::getStudentNo, studentNo);
        List<StageStatus> records = list(wrapper);

        // 按 stage 建立映射
        Map<String, StageStatus> recordMap = new HashMap<>();
        for (StageStatus record : records) {
            recordMap.put(record.getStage(), record);
        }

        // 按 STAGES 固定顺序组装返回，缺失的环节按未开始补全
        List<StageStatusVO> result = new ArrayList<>();
        for (String stage : STAGES) {
            StageStatus record = recordMap.get(stage);
            StageStatusVO vo = new StageStatusVO();
            vo.setStage(stage);
            if (record != null) {
                vo.setStatus(record.getStatus());
                vo.setStartTime(record.getStartTime());
                vo.setCompleteTime(record.getCompleteTime());
            } else {
                vo.setStatus(StageStatusType.NOT_STARTED.getCode());
            }
            vo.setStatusDesc(StageStatusType.fromCode(vo.getStatus()).getDescription());
            result.add(vo);
        }
        return result;
    }

    @Override
    public void startStage(String studentNo, String stage) {
        validateStage(stage);

        // 查询当前环节记录
        StageStatus record = getStageRecord(studentNo, stage);
        if (record != null && record.getStatus() != null
                && record.getStatus().equals(StageStatusType.IN_PROGRESS.getCode())) {
            throw new RuntimeException(stage + "环节已处于进行中，无需重复启动");
        }
        if (record != null && record.getStatus() != null
                && record.getStatus().equals(StageStatusType.COMPLETED.getCode())) {
            throw new RuntimeException(stage + "环节已完成，不可重新启动");
        }

        // 启动环节：状态置为进行中，记录开始时间
        if (record == null) {
            record = new StageStatus();
            record.setStudentNo(studentNo);
            record.setStage(stage);
            record.setStatus(StageStatusType.IN_PROGRESS.getCode());
            record.setStartTime(LocalDateTime.now());
            save(record);
        } else {
            record.setStatus(StageStatusType.IN_PROGRESS.getCode());
            record.setStartTime(LocalDateTime.now());
            updateById(record);
        }
    }

    @Override
    public void completeStage(String studentNo, String stage) {
        validateStage(stage);

        StageStatus record = getStageRecord(studentNo, stage);
        if (record == null) {
            throw new RuntimeException(stage + "环节尚未启动，无法完成");
        }
        if (record.getStatus() != null
                && record.getStatus().equals(StageStatusType.NOT_STARTED.getCode())) {
            throw new RuntimeException(stage + "环节尚未启动，无法完成");
        }
        if (record.getStatus() != null
                && record.getStatus().equals(StageStatusType.COMPLETED.getCode())) {
            throw new RuntimeException(stage + "环节已完成，无需重复操作");
        }

        record.setStatus(StageStatusType.COMPLETED.getCode());
        record.setCompleteTime(LocalDateTime.now());
        updateById(record);
    }

    @Override
    public void startBatch(String stage, Integer studentGroupId) {
        validateStage(stage);

        // 查询学生组内所有学生
        LambdaQueryWrapper<Student> studentWrapper = new LambdaQueryWrapper<>();
        studentWrapper.eq(Student::getStudentGroupId, studentGroupId);
        List<Student> students = studentMapper.selectList(studentWrapper);

        if (students.isEmpty()) {
            throw new RuntimeException("学生组不存在或组内无学生");
        }

        // 批量启动：跳过已启动或已完成的
        int successCount = 0;
        int skipCount = 0;
        for (Student student : students) {
            StageStatus record = getStageRecord(student.getStudentNo(), stage);
            if (record != null && record.getStatus() != null
                    && (record.getStatus().equals(StageStatusType.IN_PROGRESS.getCode())
                            || record.getStatus().equals(StageStatusType.COMPLETED.getCode()))) {
                skipCount++;
                continue;
            }

            if (record == null) {
                record = new StageStatus();
                record.setStudentNo(student.getStudentNo());
                record.setStage(stage);
                record.setStatus(StageStatusType.IN_PROGRESS.getCode());
                record.setStartTime(LocalDateTime.now());
                save(record);
            } else {
                record.setStatus(StageStatusType.IN_PROGRESS.getCode());
                record.setStartTime(LocalDateTime.now());
                updateById(record);
            }
            successCount++;
        }

        if (successCount == 0) {
            throw new RuntimeException("组内所有学生的" + stage + "环节已启动或已完成，无需重复操作");
        }
    }

    @Override
    public StageStatusOverviewVO getOverview(String stage, Integer studentGroupId) {
        validateStage(stage);

        // 查询学生组内所有学生在指定环节的状态
        List<StageStatusOverviewVO.StudentStageStatusItem> items = baseMapper.selectOverviewByGroup(studentGroupId,
                stage);

        // 统计各状态人数
        Map<String, Integer> statistics = new LinkedHashMap<>();
        statistics.put("未开始", 0);
        statistics.put("进行中", 0);
        statistics.put("已完成", 0);

        for (StageStatusOverviewVO.StudentStageStatusItem item : items) {
            Integer status = item.getStatus();
            if (status == null) {
                status = StageStatusType.NOT_STARTED.getCode();
                item.setStatus(status);
            }
            String desc = StageStatusType.fromCode(status).getDescription();
            item.setStatusDesc(desc);
            statistics.merge(desc, 1, Integer::sum);
        }

        StageStatusOverviewVO vo = new StageStatusOverviewVO();
        vo.setStage(stage);
        vo.setStudentGroupId(studentGroupId);
        vo.setStatistics(statistics);
        vo.setList(items);
        return vo;
    }

    // ==================== 内部方法 ====================

    /**
     * 校验环节名称合法性
     */
    private void validateStage(String stage) {
        if (!STAGES.contains(stage)) {
            throw new RuntimeException("无效的环节名称：" + stage + "，仅支持：开题、中期、答辩");
        }
    }

    /**
     * 查询学生指定环节的状态记录
     */
    private StageStatus getStageRecord(String studentNo, String stage) {
        LambdaQueryWrapper<StageStatus> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StageStatus::getStudentNo, studentNo)
                .eq(StageStatus::getStage, stage);
        return getOne(wrapper);
    }

    /**
     * 查询学生指定环节的状态码
     *
     * @return 状态码（1/2/3），null 表示无记录
     */
    private Integer getStageStatusCode(String studentNo, String stage) {
        StageStatus record = getStageRecord(studentNo, stage);
        return record != null ? record.getStatus() : null;
    }
}
