package com.example.graduationevaluationsystem.service;

import jakarta.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.Map;

/**
 * 评价手册导出 Service
 */
public interface ManualExportService {

    /**
     * 导出单个学生评价手册（Word 文件）
     */
    void exportSingle(String studentNo, HttpServletResponse response);

    /**
     * 批量导出评价手册（ZIP 压缩包）
     */
    void exportBatch(List<String> studentNos, HttpServletResponse response);

    /**
     * 按学生组批量导出评价手册（ZIP 压缩包）
     */
    void exportByGroup(Integer studentGroupId, HttpServletResponse response);

    /**
     * 获取预览数据（占位符 -> 值 的映射）
     */
    Map<String, String> getPreviewData(String studentNo);
}
