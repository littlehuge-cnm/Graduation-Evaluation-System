package com.example.graduationevaluationsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.graduationevaluationsystem.entity.Student;
import com.example.graduationevaluationsystem.service.*;
import com.example.graduationevaluationsystem.vo.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTbl;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 评价手册导出 Service 实现
 */
@Service
@RequiredArgsConstructor
public class ManualExportServiceImpl implements ManualExportService {

    private final StudentService studentService;
    private final StageStatusService stageStatusService;
    private final DocumentService documentService;
    private final ScoreRecordService scoreRecordService;
    private final StudentGroupService studentGroupService;

    @Override
    public void exportSingle(String studentNo, HttpServletResponse response) {
        Student student = studentService.getById(studentNo);
        if (student == null) {
            throw new RuntimeException("学生不存在");
        }

        try {
            byte[] docBytes = generateWordBytes(studentNo);

            String fileName = student.getStudentName() + ".docx";
            setResponseHeaders(response, fileName);
            response.getOutputStream().write(docBytes);
            response.getOutputStream().flush();
        } catch (IOException e) {
            throw new RuntimeException("导出评价手册失败: " + e.getMessage());
        }
    }

    @Override
    public void exportBatch(List<String> studentNos, HttpServletResponse response) {
        try {
            String zipFileName = "评价手册批量导出.zip";
            setResponseHeaders(response, zipFileName);

            try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream(), StandardCharsets.UTF_8)) {
                for (String studentNo : studentNos) {
                    Student student = studentService.getById(studentNo);
                    if (student == null) {
                        continue;
                    }
                    byte[] docBytes = generateWordBytes(studentNo);
                    ZipEntry entry = new ZipEntry(student.getStudentName() + ".docx");
                    zos.putNextEntry(entry);
                    zos.write(docBytes);
                    zos.closeEntry();
                }
            }
            response.getOutputStream().flush();
        } catch (IOException e) {
            throw new RuntimeException("批量导出失败: " + e.getMessage());
        }
    }

    @Override
    public void exportByGroup(Integer studentGroupId, HttpServletResponse response) {
        StudentGroupVO groupVO = studentGroupService.getGroupById(studentGroupId);
        if (groupVO == null || groupVO.getStudents() == null || groupVO.getStudents().isEmpty()) {
            throw new RuntimeException("学生组不存在或组内无学生");
        }

        try {
            String zipFileName = (groupVO.getGroupName() != null ? groupVO.getGroupName() : "学生组" + studentGroupId) + "_评价手册.zip";
            setResponseHeaders(response, zipFileName);

            try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream(), StandardCharsets.UTF_8)) {
                for (StudentGroupVO.StudentBriefVO brief : groupVO.getStudents()) {
                    Student student = studentService.getById(brief.getStudentNo());
                    if (student == null) {
                        continue;
                    }
                    byte[] docBytes = generateWordBytes(brief.getStudentNo());
                    ZipEntry entry = new ZipEntry(student.getStudentName() + ".docx");
                    zos.putNextEntry(entry);
                    zos.write(docBytes);
                    zos.closeEntry();
                }
            }
            response.getOutputStream().flush();
        } catch (IOException e) {
            throw new RuntimeException("按组导出失败: " + e.getMessage());
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 生成单个学生评价手册 Word 文档字节数组
     */
    private byte[] generateWordBytes(String studentNo) throws IOException {
        Student student = studentService.getById(studentNo);

        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // 标题
            XWPFParagraph titlePara = doc.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText("本科生毕业设计评价手册");
            titleRun.setBold(true);
            titleRun.setFontSize(18);
            titleRun.setFontFamily("宋体");

            // 空行
            doc.createParagraph();

            // 一、基本信息
            addHeading(doc, "一、基本信息");
            XWPFTable infoTable = doc.createTable(4, 4);
            setTableWidth(infoTable, 9000);
            fillCell(infoTable, 0, 0, "学号");
            fillCell(infoTable, 0, 1, student.getStudentNo());
            fillCell(infoTable, 0, 2, "姓名");
            fillCell(infoTable, 0, 3, student.getStudentName());
            fillCell(infoTable, 1, 0, "性别");
            fillCell(infoTable, 1, 1, student.getGender() != null ? student.getGender() : "");
            fillCell(infoTable, 1, 2, "班级");
            fillCell(infoTable, 1, 3, student.getClassName() != null ? student.getClassName() : "");
            fillCell(infoTable, 2, 0, "专业");
            fillCell(infoTable, 2, 1, student.getMajor() != null ? student.getMajor() : "");
            fillCell(infoTable, 2, 2, "年级");
            fillCell(infoTable, 2, 3, student.getGrade() != null ? student.getGrade() : "");
            String overallDesc = "";
            if (student.getOverallStatus() != null) {
                switch (student.getOverallStatus()) {
                    case 1 -> overallDesc = "待分配";
                    case 2 -> overallDesc = "进行中";
                    case 3 -> overallDesc = "待答辩";
                    case 4 -> overallDesc = "已完成";
                    case 5 -> overallDesc = "已弃做";
                }
            }
            fillCell(infoTable, 3, 0, "整体进度");
            fillCell(infoTable, 3, 1, overallDesc);
            fillCell(infoTable, 3, 2, "所属组号");
            fillCell(infoTable, 3, 3, student.getStudentGroupId() != null ? String.valueOf(student.getStudentGroupId()) : "");

            // 空行
            doc.createParagraph();

            // 二、指导/评阅教师
            addHeading(doc, "二、指导/评阅教师");
            StudentTeacherVO teachers = studentService.getTeachersByStudentNo(studentNo);
            XWPFTable teacherTable = doc.createTable(2, 2);
            setTableWidth(teacherTable, 9000);
            fillCell(teacherTable, 0, 0, "指导教师");
            fillCell(teacherTable, 0, 1, teachers.getSupervisor() != null
                    ? teachers.getSupervisor().getTeacherName() + "（" + teachers.getSupervisor().getTeacherNo() + "）"
                    : "未分配");
            fillCell(teacherTable, 1, 0, "评阅教师");
            fillCell(teacherTable, 1, 1, teachers.getReviewer() != null
                    ? teachers.getReviewer().getTeacherName() + "（" + teachers.getReviewer().getTeacherNo() + "）"
                    : "未分配");

            // 空行
            doc.createParagraph();

            // 三、环节状态
            addHeading(doc, "三、环节状态");
            List<StageStatusVO> stageStatuses = stageStatusService.getStudentStageStatus(studentNo);
            XWPFTable stageTable = doc.createTable(stageStatuses.size() + 1, 4);
            setTableWidth(stageTable, 9000);
            fillCell(stageTable, 0, 0, "环节");
            fillCell(stageTable, 0, 1, "状态");
            fillCell(stageTable, 0, 2, "开始时间");
            fillCell(stageTable, 0, 3, "完成时间");
            for (int i = 0; i < stageStatuses.size(); i++) {
                StageStatusVO s = stageStatuses.get(i);
                fillCell(stageTable, i + 1, 0, s.getStage());
                fillCell(stageTable, i + 1, 1, s.getStatusDesc());
                fillCell(stageTable, i + 1, 2, s.getStartTime() != null ? s.getStartTime().toString() : "");
                fillCell(stageTable, i + 1, 3, s.getCompleteTime() != null ? s.getCompleteTime().toString() : "");
            }

            // 空行
            doc.createParagraph();

            // 四、文档信息
            addHeading(doc, "四、文档信息");
            List<DocumentVO> documents = documentService.getDocsByStudentNo(studentNo, null);
            if (documents.isEmpty()) {
                XWPFParagraph p = doc.createParagraph();
                p.createRun().setText("暂无文档记录");
            } else {
                XWPFTable docTable = doc.createTable(documents.size() + 1, 4);
                setTableWidth(docTable, 9000);
                fillCell(docTable, 0, 0, "文档类型");
                fillCell(docTable, 0, 1, "题目");
                fillCell(docTable, 0, 2, "文档状态");
                fillCell(docTable, 0, 3, "审批状态");
                for (int i = 0; i < documents.size(); i++) {
                    DocumentVO d = documents.get(i);
                    fillCell(docTable, i + 1, 0, d.getDocType());
                    fillCell(docTable, i + 1, 1, d.getTitle() != null ? d.getTitle() : "");
                    fillCell(docTable, i + 1, 2, d.getStatusDesc());
                    fillCell(docTable, i + 1, 3, d.getApprovalStatusDesc() != null ? d.getApprovalStatusDesc() : "");
                }
            }

            // 空行
            doc.createParagraph();

            // 五、评价记录
            addHeading(doc, "五、评价记录");
            List<ScoreRecordVO> records = scoreRecordService.getRecordsByStudentNo(studentNo, null);
            if (records.isEmpty()) {
                XWPFParagraph p = doc.createParagraph();
                p.createRun().setText("暂无评价记录");
            } else {
                XWPFTable recTable = doc.createTable(records.size() + 1, 5);
                setTableWidth(recTable, 9000);
                fillCell(recTable, 0, 0, "条目类型");
                fillCell(recTable, 0, 1, "分项成绩");
                fillCell(recTable, 0, 2, "总成绩");
                fillCell(recTable, 0, 3, "等级");
                fillCell(recTable, 0, 4, "状态");
                for (int i = 0; i < records.size(); i++) {
                    ScoreRecordVO r = records.get(i);
                    fillCell(recTable, i + 1, 0, r.getItemType());
                    fillCell(recTable, i + 1, 1, r.getSubScores() != null ? r.getSubScores() : "");
                    fillCell(recTable, i + 1, 2, r.getScore() != null ? r.getScore().toString() : "");
                    fillCell(recTable, i + 1, 3, r.getGrade() != null ? r.getGrade() : "");
                    fillCell(recTable, i + 1, 4, r.getRecordStatusDesc());
                }

                // 评语详情
                doc.createParagraph();
                addHeading(doc, "评语详情");
                for (ScoreRecordVO r : records) {
                    if (r.getComment() != null && !r.getComment().isEmpty()) {
                        XWPFParagraph p = doc.createParagraph();
                        XWPFRun run = p.createRun();
                        run.setText(r.getItemType() + "：" + r.getComment());
                        run.setFontSize(10);
                        run.setFontFamily("宋体");
                    }
                }
            }

            doc.write(baos);
            return baos.toByteArray();
        }
    }

    /**
     * 添加二级标题
     */
    private void addHeading(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontSize(14);
        run.setFontFamily("宋体");
    }

    /**
     * 填充表格单元格
     */
    private void fillCell(XWPFTable table, int row, int col, String text) {
        XWPFTableRow tableRow = table.getRow(row);
        if (tableRow == null) {
            tableRow = table.createRow();
        }
        XWPFTableCell cell = tableRow.getCell(col);
        if (cell == null) {
            cell = tableRow.addNewTableCell();
        }
        cell.removeParagraph(0);
        XWPFParagraph p = cell.addParagraph();
        XWPFRun run = p.createRun();
        run.setText(text != null ? text : "");
        run.setFontSize(10);
        run.setFontFamily("宋体");
    }

    /**
     * 设置表格宽度
     */
    private void setTableWidth(XWPFTable table, int widthTwips) {
        CTTbl ctTbl = table.getCTTbl();
        CTTblPr tblPr = ctTbl.getTblPr() == null ? ctTbl.addNewTblPr() : ctTbl.getTblPr();
        CTTblWidth tblWidth = tblPr.isSetTblW() ? tblPr.getTblW() : tblPr.addNewTblW();
        tblWidth.setW(widthTwips);
        tblWidth.setType(STTblWidth.DXA);
    }

    /**
     * 设置 HTTP 响应头
     */
    private void setResponseHeaders(HttpServletResponse response, String fileName) throws UnsupportedEncodingException {
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);
        response.setCharacterEncoding("UTF-8");
    }
}
