package com.example.graduationevaluationsystem.service;

import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

/**
 * 评价手册导出 Service
 */
public interface ManualExportService {

    /**
     * 导出单个学生评价手册（Word 文件）
     *
     * @param studentNo 学号
     * @param response  HTTP 响应
     */
    void exportSingle(String studentNo, HttpServletResponse response);

    /**
     * 批量导出评价手册（ZIP 压缩包）
     *
     * @param studentNos 学号列表
     * @param response   HTTP 响应
     */
    void exportBatch(List<String> studentNos, HttpServletResponse response);

    /**
     * 按学生组批量导出评价手册（ZIP 压缩包）
     *
     * @param studentGroupId 学生组号
     * @param response       HTTP 响应
     */
    void exportByGroup(Integer studentGroupId, HttpServletResponse response);
}
