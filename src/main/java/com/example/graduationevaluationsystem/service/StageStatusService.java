package com.example.graduationevaluationsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.graduationevaluationsystem.entity.StageStatus;
import com.example.graduationevaluationsystem.vo.StageStatusOverviewVO;
import com.example.graduationevaluationsystem.vo.StageStatusVO;

import java.util.List;

/**
 * 环节状态 Service
 */
public interface StageStatusService extends IService<StageStatus> {

    /**
     * 查询学生环节状态
     *
     * @param studentNo 学号
     * @return 三个环节的状态列表（含状态描述）
     */
    List<StageStatusVO> getStudentStageStatus(String studentNo);

    /**
     * 启动环节（1→2）
     * <p>
     * 前置环节必须为已完成（3）。开题无前置环节。
     *
     * @param studentNo 学号
     * @param stage     环节（开题/中期/答辩）
     */
    void startStage(String studentNo, String stage);

    /**
     * 完成环节（2→3）
     *
     * @param studentNo 学号
     * @param stage     环节（开题/中期/答辩）
     */
    void completeStage(String studentNo, String stage);

    /**
     * 批量启动环节
     * <p>
     * 按学生组批量将指定环节从未开始（1）启动为进行中（2）。
     *
     * @param stage          环节（开题/中期/答辩）
     * @param studentGroupId 学生组号
     */
    void startBatch(String stage, Integer studentGroupId);

    /**
     * 查询环节进度总览
     *
     * @param stage          环节（开题/中期/答辩）
     * @param studentGroupId 学生组号
     * @return 进度总览（含统计和明细）
     */
    StageStatusOverviewVO getOverview(String stage, Integer studentGroupId);
}
