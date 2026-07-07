package com.example.graduationevaluationsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.graduationevaluationsystem.entity.ScoreRecord;
import com.example.graduationevaluationsystem.vo.ScoreRecordTodoVO;
import com.example.graduationevaluationsystem.vo.ScoreRecordVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 评价记录 Service
 */
public interface ScoreRecordService extends IService<ScoreRecord> {

    /**
     * 录入评价记录
     * <p>
     * 校验 itemType 合法性、分项成绩格式、重复录入；
     * recorderNo 和 recordTime 由调用方传入（后续接入 JWT 后从上下文获取）。
     *
     * @param studentNo     学号
     * @param itemType      条目类型
     * @param subScores     分项成绩（逗号分隔）
     * @param score         总成绩（为 null 时按分项求和）
     * @param grade         等级
     * @param comment       评语/记录内容
     * @param recordStatus  记录状态（为 null 时默认 1=暂存）
     * @param recorderNo    录入人账号
     * @return 记录编号
     */
    Integer createRecord(String studentNo, String itemType, String subScores,
                         BigDecimal score, String grade, String comment,
                         Integer recordStatus, String recorderNo);

    /**
     * 修改评价记录
     * <p>
     * 仅暂存状态可修改；已确认需先解锁。
     *
     * @param id            记录编号
     * @param subScores     分项成绩
     * @param score         总成绩
     * @param grade         等级
     * @param comment       评语/记录内容
     * @param recordStatus  记录状态
     */
    void updateRecord(Integer id, String subScores, BigDecimal score,
                      String grade, String comment, Integer recordStatus);

    /**
     * 确认评价记录（暂存→已确认，锁定不可改）
     *
     * @param id 记录编号
     */
    void confirmRecord(Integer id);

    /**
     * 解锁评价记录（已确认→暂存，允许修改）
     *
     * @param id 记录编号
     */
    void unlockRecord(Integer id);

    /**
     * 按编号查询评价记录详情（含录入人姓名、状态描述）
     *
     * @param id 记录编号
     * @return 评价记录详情
     */
    ScoreRecordVO getRecordById(Integer id);

    /**
     * 按学号查询评价记录列表，可按条目类型过滤
     *
     * @param studentNo 学号
     * @param itemType  条目类型（可为空）
     * @return 评价记录列表
     */
    List<ScoreRecordVO> getRecordsByStudentNo(String studentNo, String itemType);

    /**
     * 查询待录入列表（按角色）
     * <p>
     * 教师调用时按其身份（组长/秘书/指导教师/评阅教师）返回对应待录入项；
     * 管理员调用时返回待委员会评定的学生列表。
     *
     * @param recorderNo 录入人账号
     * @param userType   用户类型（teacher/admin）
     * @return 待录入列表
     */
    List<ScoreRecordTodoVO> getTodoList(String recorderNo, String userType);
}
