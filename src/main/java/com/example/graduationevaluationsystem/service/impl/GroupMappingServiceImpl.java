package com.example.graduationevaluationsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.graduationevaluationsystem.common.exception.BusinessException;
import com.example.graduationevaluationsystem.dto.GroupMappingBatchAssignDTO;
import com.example.graduationevaluationsystem.entity.GroupMapping;
import com.example.graduationevaluationsystem.entity.StudentGroup;
import com.example.graduationevaluationsystem.entity.TeacherGroup;
import com.example.graduationevaluationsystem.mapper.GroupMappingMapper;
import com.example.graduationevaluationsystem.mapper.StudentGroupMapper;
import com.example.graduationevaluationsystem.mapper.TeacherGroupMapper;
import com.example.graduationevaluationsystem.service.GroupMappingService;
import com.example.graduationevaluationsystem.vo.GroupMappingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

/**
 * 环节对应关系 Service 实现
 */
@Service
@RequiredArgsConstructor
public class GroupMappingServiceImpl extends ServiceImpl<GroupMappingMapper, GroupMapping>
        implements GroupMappingService {

    private final TeacherGroupMapper teacherGroupMapper;
    private final StudentGroupMapper studentGroupMapper;

    private static final Set<String> VALID_STAGES = Set.of("开题", "中期", "答辩");

    @Override
    public void createMapping(String stage, Integer teacherGroupId, Integer studentGroupId) {
        validateStage(stage);
        validateGroupsExist(teacherGroupId, studentGroupId);
        validateNotDuplicate(stage, teacherGroupId, studentGroupId, null);

        GroupMapping mapping = new GroupMapping();
        mapping.setStage(stage);
        mapping.setTeacherGroupId(teacherGroupId);
        mapping.setStudentGroupId(studentGroupId);
        save(mapping);
    }

    @Override
    public void updateMapping(Integer id, String stage, Integer teacherGroupId, Integer studentGroupId) {
        GroupMapping existing = getById(id);
        if (existing == null) {
            throw new RuntimeException("环节对应关系不存在");
        }
        validateStage(stage);
        validateGroupsExist(teacherGroupId, studentGroupId);
        validateNotDuplicate(stage, teacherGroupId, studentGroupId, id);

        existing.setStage(stage);
        existing.setTeacherGroupId(teacherGroupId);
        existing.setStudentGroupId(studentGroupId);
        updateById(existing);
    }

    @Override
    public List<GroupMappingVO> getListByStage(String stage) {
        return baseMapper.selectMappingList(stage);
    }

    @Override
    public GroupMappingVO getMappingById(Integer id) {
        return baseMapper.selectMappingById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<GroupMappingVO> randomAssignAll() {
        // 1. 查询全部教师组和学生组
        List<TeacherGroup> teacherGroups = teacherGroupMapper.selectList(null);
        List<StudentGroup> studentGroups = studentGroupMapper.selectList(null);

        int teacherCount = teacherGroups.size();
        int studentCount = studentGroups.size();

        // 2. 边界校验
        if (teacherCount < 3) {
            throw new RuntimeException("教师组数量不足，至少需要 3 个教师组，当前：" + teacherCount);
        }
        if (teacherCount < studentCount) {
            throw new RuntimeException("教师组数量不能少于学生组数量，教师组：" + teacherCount + "，学生组：" + studentCount);
        }

        // 3. 提取 groupId 列表
        List<Integer> teacherGroupIds = teacherGroups.stream()
                .map(TeacherGroup::getGroupId)
                .toList();
        List<Integer> studentGroupIds = studentGroups.stream()
                .map(StudentGroup::getGroupId)
                .toList();

        // 4. 执行随机分配算法
        Integer[][] assignment = doRandomAssign(teacherGroupIds, studentGroupIds);

        // 5. 清除原有全部对应关系
        baseMapper.deleteAll();

        // 6. 批量插入新对应关系
        String[] stages = { "开题", "中期", "答辩" };
        List<GroupMapping> mappings = new ArrayList<>();
        for (int stageIdx = 0; stageIdx < 3; stageIdx++) {
            for (int studentIdx = 0; studentIdx < studentCount; studentIdx++) {
                GroupMapping mapping = new GroupMapping();
                mapping.setStage(stages[stageIdx]);
                mapping.setTeacherGroupId(assignment[studentIdx][stageIdx]);
                mapping.setStudentGroupId(studentGroupIds.get(studentIdx));
                mappings.add(mapping);
            }
        }
        saveBatch(mappings);

        // 7. 返回含组名的完整列表
        return baseMapper.selectMappingList(null);
    }

    /**
     * 随机分配算法（贪心 + 随机重试）
     * <p>
     * 逐阶段处理，每个阶段内随机打乱学生组顺序，为每个学生组从"该阶段可用且未在该学生组
     * 前序阶段出现过的教师组"中随机选一个。若某次尝试失败则整体重来，最多重试 1000 次。
     *
     * @param teacherGroupIds 教师组 groupId 列表
     * @param studentGroupIds 学生组 groupId 列表
     * @return assignment[studentIndex][stageIndex] = teacherGroupId
     */
    private Integer[][] doRandomAssign(List<Integer> teacherGroupIds, List<Integer> studentGroupIds) {
        final int MAX_RETRIES = 1000;
        int teacherCount = teacherGroupIds.size();
        int studentCount = studentGroupIds.size();

        for (int retry = 0; retry < MAX_RETRIES; retry++) {
            // assignment[studentIndex][stageIndex] = teacherGroupId
            Integer[][] assignment = new Integer[studentCount][3];
            // usedInStage[stageIndex] = 该阶段已被占用的教师组集合
            Set<Integer>[] usedInStage = new Set[3];
            // assignedToStudent[studentIndex] = 该学生组在所有阶段已分配的教师组集合
            Set<Integer>[] assignedToStudent = new Set[studentCount];

            for (int s = 0; s < 3; s++) {
                usedInStage[s] = new HashSet<>();
            }
            for (int i = 0; i < studentCount; i++) {
                assignedToStudent[i] = new HashSet<>();
            }

            boolean success = true;

            for (int stageIdx = 0; stageIdx < 3 && success; stageIdx++) {
                // 随机打乱学生组处理顺序
                List<Integer> studentOrder = new ArrayList<>();
                for (int i = 0; i < studentCount; i++) {
                    studentOrder.add(i);
                }
                Collections.shuffle(studentOrder, ThreadLocalRandom.current());

                for (int studentIdx : studentOrder) {
                    // 计算可用教师组：未在本阶段使用 且 未分配给该学生组（前序阶段）
                    List<Integer> candidates = new ArrayList<>();
                    for (int teacherIdx = 0; teacherIdx < teacherCount; teacherIdx++) {
                        Integer teacherGroupId = teacherGroupIds.get(teacherIdx);
                        if (!usedInStage[stageIdx].contains(teacherGroupId)
                                && !assignedToStudent[studentIdx].contains(teacherGroupId)) {
                            candidates.add(teacherGroupId);
                        }
                    }

                    if (candidates.isEmpty()) {
                        success = false;
                        break;
                    }

                    // 随机选一个
                    Integer chosen = candidates.get(
                            ThreadLocalRandom.current().nextInt(candidates.size()));
                    assignment[studentIdx][stageIdx] = chosen;
                    usedInStage[stageIdx].add(chosen);
                    assignedToStudent[studentIdx].add(chosen);
                }
            }

            if (success) {
                return assignment;
            }
        }

        throw new RuntimeException("随机分配失败，已尝试 " + MAX_RETRIES + " 次仍未找到合法方案，请重试");
    }

    /**
     * 校验环节是否合法
     */
    private void validateStage(String stage) {
        if (!VALID_STAGES.contains(stage)) {
            throw new RuntimeException("环节不合法，仅支持：开题/中期/答辩");
        }
    }

    /**
     * 校验教师组和学生组是否存在
     */
    private void validateGroupsExist(Integer teacherGroupId, Integer studentGroupId) {
        if (teacherGroupMapper.selectById(teacherGroupId) == null) {
            throw new RuntimeException("教师分组不存在：" + teacherGroupId);
        }
        if (studentGroupMapper.selectById(studentGroupId) == null) {
            throw new RuntimeException("学生分组不存在：" + studentGroupId);
        }
    }

    /**
     * 校验同一环节下教师组/学生组是否已被占用（排除自身）
     */
    private void validateNotDuplicate(String stage, Integer teacherGroupId, Integer studentGroupId, Integer excludeId) {
        // 检查教师组在该环节下是否已对应其他学生组
        LambdaQueryWrapper<GroupMapping> teacherWrapper = new LambdaQueryWrapper<>();
        teacherWrapper.eq(GroupMapping::getStage, stage)
                .eq(GroupMapping::getTeacherGroupId, teacherGroupId);
        if (excludeId != null) {
            teacherWrapper.ne(GroupMapping::getId, excludeId);
        }
        if (count(teacherWrapper) > 0) {
            throw new RuntimeException("该环节下教师组[" + teacherGroupId + "]已存在对应关系");
        }

        // 检查学生组在该环节下是否已对应其他教师组
        LambdaQueryWrapper<GroupMapping> studentWrapper = new LambdaQueryWrapper<>();
        studentWrapper.eq(GroupMapping::getStage, stage)
                .eq(GroupMapping::getStudentGroupId, studentGroupId);
        if (excludeId != null) {
            studentWrapper.ne(GroupMapping::getId, excludeId);
        }
        if (count(studentWrapper) > 0) {
            throw new RuntimeException("该环节下学生组[" + studentGroupId + "]已存在对应关系");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAssign(List<GroupMappingBatchAssignDTO> list) {
        if (list == null || list.isEmpty()) {
            throw new BusinessException("分配列表不能为空");
        }

        Map<String, Function<GroupMappingBatchAssignDTO, Integer>> stageGetters = new LinkedHashMap<>();
        stageGetters.put("开题", GroupMappingBatchAssignDTO::getOpeningTeacherGroupId);
        stageGetters.put("中期", GroupMappingBatchAssignDTO::getMidtermTeacherGroupId);
        stageGetters.put("答辩", GroupMappingBatchAssignDTO::getDefenseTeacherGroupId);

        // 1. 校验学生组存在，且同一学生组的三个环节教师组互不相同
        for (GroupMappingBatchAssignDTO dto : list) {
            Integer studentGroupId = dto.getStudentGroupId();
            StudentGroup studentGroup = studentGroupMapper.selectById(studentGroupId);
            if (studentGroup == null) {
                throw new BusinessException("学生分组不存在：" + studentGroupId);
            }
            Set<Integer> selected = new HashSet<>();
            for (Map.Entry<String, Function<GroupMappingBatchAssignDTO, Integer>> entry : stageGetters.entrySet()) {
                Integer teacherGroupId = entry.getValue().apply(dto);
                if (teacherGroupId == null) {
                    continue;
                }
                if (teacherGroupMapper.selectById(teacherGroupId) == null) {
                    throw new BusinessException("教师分组不存在：" + teacherGroupId);
                }
                if (!selected.add(teacherGroupId)) {
                    throw new BusinessException("学生组【" + studentGroup.getGroupName() + "】在三个环节不能分配相同的教师组");
                }
            }
        }

        // 2. 校验批量列表内部：同一环节中教师组不能分配给多个学生组
        for (Map.Entry<String, Function<GroupMappingBatchAssignDTO, Integer>> entry : stageGetters.entrySet()) {
            String stage = entry.getKey();
            Map<Integer, Integer> teacherToStudent = new HashMap<>();
            for (GroupMappingBatchAssignDTO dto : list) {
                Integer studentGroupId = dto.getStudentGroupId();
                Integer teacherGroupId = entry.getValue().apply(dto);
                if (teacherGroupId == null) {
                    continue;
                }
                if (teacherToStudent.containsKey(teacherGroupId)) {
                    throw new BusinessException("环节【" + stage + "】中教师组不能同时分配给多个学生组");
                }
                teacherToStudent.put(teacherGroupId, studentGroupId);
            }
        }

        // 3. 校验与数据库已有数据是否冲突
        List<Integer> studentGroupIds = list.stream()
                .map(GroupMappingBatchAssignDTO::getStudentGroupId)
                .toList();
        for (Map.Entry<String, Function<GroupMappingBatchAssignDTO, Integer>> entry : stageGetters.entrySet()) {
            String stage = entry.getKey();
            for (GroupMappingBatchAssignDTO dto : list) {
                Integer teacherGroupId = entry.getValue().apply(dto);
                if (teacherGroupId == null) {
                    continue;
                }
                LambdaQueryWrapper<GroupMapping> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(GroupMapping::getStage, stage)
                        .eq(GroupMapping::getTeacherGroupId, teacherGroupId)
                        .notIn(GroupMapping::getStudentGroupId, studentGroupIds);
                if (count(wrapper) > 0) {
                    TeacherGroup tg = teacherGroupMapper.selectById(teacherGroupId);
                    throw new BusinessException("环节【" + stage + "】中教师组【"
                            + (tg != null ? tg.getGroupName() : teacherGroupId) + "】已分配给其他学生组");
                }
            }
        }

        // 4. 保存/更新/删除每个环节对应关系
        for (Map.Entry<String, Function<GroupMappingBatchAssignDTO, Integer>> entry : stageGetters.entrySet()) {
            String stage = entry.getKey();
            for (GroupMappingBatchAssignDTO dto : list) {
                Integer studentGroupId = dto.getStudentGroupId();
                Integer teacherGroupId = entry.getValue().apply(dto);
                LambdaQueryWrapper<GroupMapping> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(GroupMapping::getStage, stage).eq(GroupMapping::getStudentGroupId, studentGroupId);
                GroupMapping existing = getOne(wrapper);
                if (teacherGroupId == null) {
                    if (existing != null) {
                        removeById(existing.getId());
                    }
                } else {
                    if (existing == null) {
                        GroupMapping mapping = new GroupMapping();
                        mapping.setStage(stage);
                        mapping.setStudentGroupId(studentGroupId);
                        mapping.setTeacherGroupId(teacherGroupId);
                        save(mapping);
                    } else {
                        existing.setTeacherGroupId(teacherGroupId);
                        updateById(existing);
                    }
                }
            }
        }
    }
}
