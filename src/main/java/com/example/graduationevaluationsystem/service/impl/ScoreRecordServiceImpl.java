package com.example.graduationevaluationsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.graduationevaluationsystem.common.enums.ItemType;
import com.example.graduationevaluationsystem.common.enums.RecordStatus;
import com.example.graduationevaluationsystem.entity.ScoreRecord;
import com.example.graduationevaluationsystem.mapper.ScoreRecordMapper;
import com.example.graduationevaluationsystem.service.ScoreRecordService;
import com.example.graduationevaluationsystem.vo.ScoreRecordTodoVO;
import com.example.graduationevaluationsystem.vo.ScoreRecordVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 评价记录 Service 实现
 */
@Service
public class ScoreRecordServiceImpl extends ServiceImpl<ScoreRecordMapper, ScoreRecord> implements ScoreRecordService {

    /**
     * 角色 → 负责录入的条目类型列表
     */
    private static final Map<String, List<ItemType>> ROLE_ITEM_TYPES = Map.of(
            "组长", List.of(ItemType.OPENING_SCORE, ItemType.MIDTERM_CHECK, ItemType.DEFENSE_SCORE),
            "秘书", List.of(ItemType.DEFENSE_RECORD),
            "指导教师", List.of(ItemType.TRANSLATION, ItemType.SUPERVISION_COMMENT),
            "评阅教师", List.of(ItemType.REVIEW_COMMENT)
    );

    @Override
    public Integer createRecord(String studentNo, String itemType, String subScores,
                                BigDecimal score, String grade, String comment,
                                Integer recordStatus, String recorderNo) {
        // 1. 校验条目类型合法
        ItemType type = ItemType.fromName(itemType);

        // 2. 校验分项成绩格式
        type.validateSubScores(subScores);

        // 3. 校验环节状态
        validateStageStatus(studentNo, type);

        // 4. 校验重复录入
        LambdaQueryWrapper<ScoreRecord> dupWrapper = new LambdaQueryWrapper<>();
        dupWrapper.eq(ScoreRecord::getStudentNo, studentNo)
                  .eq(ScoreRecord::getItemType, itemType);
        if (count(dupWrapper) > 0) {
            throw new RuntimeException("该学生已存在" + itemType + "记录，请修改而非重复录入");
        }

        // 5. 计算总成绩
        BigDecimal finalScore = resolveScore(type, subScores, score);

        // 6. 委员会评定：未传 score 时自动加权计算
        if (type == ItemType.COMMITTEE_EVALUATION && finalScore == null) {
            finalScore = calculateCommitteeScore(studentNo);
        }

        // 7. 构建实体
        ScoreRecord record = new ScoreRecord();
        record.setStudentNo(studentNo);
        record.setItemType(itemType);
        record.setSubScores(subScores);
        record.setScore(finalScore);
        record.setGrade(grade);
        record.setComment(comment);
        record.setRecorderNo(recorderNo);
        record.setRecordTime(LocalDateTime.now());
        record.setRecordStatus(recordStatus != null ? recordStatus : RecordStatus.DRAFT.getCode());

        save(record);
        return record.getId();
    }

    @Override
    public void updateRecord(Integer id, String subScores, BigDecimal score,
                             String grade, String comment, Integer recordStatus) {
        ScoreRecord existing = getById(id);
        if (existing == null) {
            throw new RuntimeException("评价记录不存在");
        }

        // 仅暂存状态可修改
        if (existing.getRecordStatus() != null
                && existing.getRecordStatus() == RecordStatus.CONFIRMED.getCode()) {
            throw new RuntimeException("该记录已确认，不可修改，如需修改请联系管理员解锁");
        }

        // 校验环节状态
        ItemType type = ItemType.fromName(existing.getItemType());
        validateStageStatus(existing.getStudentNo(), type);

        // 校验分项成绩（如果传了 subScores）
        if (subScores != null) {
            type.validateSubScores(subScores);
            existing.setSubScores(subScores);
        }

        // 计算总成绩
        BigDecimal finalScore = resolveScore(type, existing.getSubScores(), score);
        if (score != null) {
            existing.setScore(score);
        } else if (subScores != null) {
            existing.setScore(finalScore);
        }

        // 委员会评定：未传 score 时自动加权计算
        if (type == ItemType.COMMITTEE_EVALUATION && existing.getScore() == null) {
            existing.setScore(calculateCommitteeScore(existing.getStudentNo()));
        }

        if (grade != null) {
            existing.setGrade(grade);
        }
        if (comment != null) {
            existing.setComment(comment);
        }
        if (recordStatus != null) {
            existing.setRecordStatus(recordStatus);
        }
        existing.setUpdateTime(LocalDateTime.now());

        updateById(existing);
    }

    @Override
    public void confirmRecord(Integer id) {
        ScoreRecord existing = getById(id);
        if (existing == null) {
            throw new RuntimeException("评价记录不存在");
        }
        if (existing.getRecordStatus() == null
                || existing.getRecordStatus() != RecordStatus.DRAFT.getCode()) {
            throw new RuntimeException("仅暂存状态的记录可以确认");
        }
        existing.setRecordStatus(RecordStatus.CONFIRMED.getCode());
        existing.setUpdateTime(LocalDateTime.now());
        updateById(existing);
    }

    @Override
    public void unlockRecord(Integer id) {
        ScoreRecord existing = getById(id);
        if (existing == null) {
            throw new RuntimeException("评价记录不存在");
        }
        if (existing.getRecordStatus() == null
                || existing.getRecordStatus() != RecordStatus.CONFIRMED.getCode()) {
            throw new RuntimeException("仅已确认的记录可以解锁");
        }
        existing.setRecordStatus(RecordStatus.DRAFT.getCode());
        existing.setUpdateTime(LocalDateTime.now());
        updateById(existing);
    }

    @Override
    public ScoreRecordVO getRecordById(Integer id) {
        ScoreRecordVO vo = baseMapper.selectRecordById(id);
        if (vo != null) {
            fillStatusDesc(vo);
        }
        return vo;
    }

    @Override
    public List<ScoreRecordVO> getRecordsByStudentNo(String studentNo, String itemType) {
        List<ScoreRecordVO> list = baseMapper.selectRecordsByStudentNo(studentNo, itemType);
        list.forEach(this::fillStatusDesc);
        return list;
    }

    @Override
    public List<ScoreRecordTodoVO> getTodoList(String recorderNo, String userType) {
        if ("admin".equals(userType)) {
            // 管理员：返回待委员会评定的学生
            return baseMapper.selectCommitteeTodoStudents();
        }

        // 教师：按角色返回待录入项
        // 1. 查询教师的关联学生及角色
        List<ScoreRecordTodoVO> studentRoles = baseMapper.selectTeacherStudentsWithRoles(recorderNo);
        if (studentRoles.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 按学生分组，收集每个学生的角色
        Map<String, ScoreRecordTodoVO> studentMap = new LinkedHashMap<>();
        Map<String, Set<String>> studentRolesMap = new LinkedHashMap<>();
        for (ScoreRecordTodoVO item : studentRoles) {
            studentMap.putIfAbsent(item.getStudentNo(), item);
            studentRolesMap.computeIfAbsent(item.getStudentNo(), k -> new LinkedHashSet<>())
                           .add(item.getRecorderRole());
        }

        // 3. 查询这些学生的全部已有评价记录
        List<String> studentNos = new ArrayList<>(studentMap.keySet());
        LambdaQueryWrapper<ScoreRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.in(ScoreRecord::getStudentNo, studentNos);
        List<ScoreRecord> existingRecords = list(recordWrapper);

        // 按 (studentNo, itemType) 分组
        Map<String, Map<String, ScoreRecord>> recordIndex = new HashMap<>();
        for (ScoreRecord r : existingRecords) {
            recordIndex.computeIfAbsent(r.getStudentNo(), k -> new HashMap<>())
                       .put(r.getItemType(), r);
        }

        // 4. 为每个学生按角色生成待录入项
        List<ScoreRecordTodoVO> todoList = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : studentRolesMap.entrySet()) {
            String studentNo = entry.getKey();
            Set<String> roles = entry.getValue();
            ScoreRecordTodoVO studentInfo = studentMap.get(studentNo);

            // 收集该学生需要录入的全部条目类型
            Set<ItemType> requiredTypes = new LinkedHashSet<>();
            for (String role : roles) {
                List<ItemType> types = ROLE_ITEM_TYPES.get(role);
                if (types != null) {
                    requiredTypes.addAll(types);
                }
            }

            // 逐个检查是否已录入
            Map<String, ScoreRecord> studentRecords = recordIndex.getOrDefault(studentNo, Collections.emptyMap());
            for (ItemType type : requiredTypes) {
                ScoreRecord existing = studentRecords.get(type.getName());
                ScoreRecordTodoVO todo = new ScoreRecordTodoVO();
                todo.setStudentNo(studentNo);
                todo.setStudentName(studentInfo.getStudentName());
                todo.setClassName(studentInfo.getClassName());
                todo.setItemType(type.getName());
                todo.setRecorderRole(type.getRecorderRole());

                if (existing == null) {
                    // 尚未录入
                    todo.setRecordStatusDesc("未录入");
                } else {
                    // 已有记录
                    todo.setRecordId(existing.getId());
                    todo.setRecordStatus(existing.getRecordStatus());
                    if (existing.getRecordStatus() != null
                            && existing.getRecordStatus() == RecordStatus.CONFIRMED.getCode()) {
                        // 已确认 → 不属于待录入
                        continue;
                    }
                    todo.setRecordStatusDesc("暂存");
                }
                todoList.add(todo);
            }
        }

        return todoList;
    }

    // ==================== 内部方法 ====================

    /**
     * 校验环节状态是否满足录入要求
     */
    private void validateStageStatus(String studentNo, ItemType type) {
        Integer requiredStatus = type.getRequiredStageStatus();
        if (requiredStatus == null) {
            return; // 不强制校验
        }

        Integer currentStatus = baseMapper.selectStageStatus(studentNo, type.getStage());
        if (currentStatus == null) {
            throw new RuntimeException("学生" + studentNo + "的" + type.getStage() + "环节状态不存在");
        }
        if (!currentStatus.equals(requiredStatus)) {
            String requiredDesc = requiredStatus == 2 ? "进行中" : "已完成";
            String currentDesc = switch (currentStatus) {
                case 1 -> "未开始";
                case 2 -> "进行中";
                case 3 -> "已完成";
                default -> "未知";
            };
            throw new RuntimeException(type.getName() + "需要" + type.getStage() + "环节处于" + requiredDesc
                    + "，当前状态为" + currentDesc);
        }
    }

    /**
     * 委员会评定加权计算
     * 公式：开题×12% + 翻译×3% + 中期×15% + 指导×15% + 评阅×15% + 答辩×40%
     */
    private BigDecimal calculateCommitteeScore(String studentNo) {
        List<Map<String, Object>> scores = baseMapper.selectConfirmedScores(studentNo);

        // 转为 itemType → score 映射
        Map<String, BigDecimal> scoreMap = new HashMap<>();
        for (Map<String, Object> row : scores) {
            String itemType = (String) row.get("item_type");
            Object scoreObj = row.get("score");
            if (scoreObj instanceof BigDecimal bd) {
                scoreMap.put(itemType, bd);
            } else if (scoreObj instanceof Number num) {
                scoreMap.put(itemType, BigDecimal.valueOf(num.doubleValue()));
            }
        }

        // 检查六项成绩是否齐全
        double total = 0;
        for (ItemType type : ItemType.values()) {
            Double weight = type.getWeight();
            if (weight == null) {
                continue;
            }
            BigDecimal score = scoreMap.get(type.getName());
            if (score == null) {
                throw new RuntimeException("委员会评定需要" + type.getName() + "已确认，请先完成录入");
            }
            total += score.doubleValue() * weight;
        }

        return BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 解析总成绩：
     * - 有分项类型且传了 score → 直接使用 score
     * - 有分项类型且未传 score → 按分项求和
     * - 无分项类型 → 直接使用 score（可为 null）
     */
    private BigDecimal resolveScore(ItemType type, String subScores, BigDecimal score) {
        if (score != null) {
            return score;
        }
        if (type.hasSubScores() && subScores != null && !subScores.isBlank()) {
            return BigDecimal.valueOf(type.calculateScore(subScores));
        }
        return null;
    }

    /**
     * 填充记录状态描述
     */
    private void fillStatusDesc(ScoreRecordVO vo) {
        if (vo.getRecordStatus() != null) {
            vo.setRecordStatusDesc(RecordStatus.fromCode(vo.getRecordStatus()).getDescription());
        }
    }
}
