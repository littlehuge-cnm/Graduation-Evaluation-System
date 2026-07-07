package com.example.graduationevaluationsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.graduationevaluationsystem.common.enums.OverallStatus;
import com.example.graduationevaluationsystem.entity.Student;
import com.example.graduationevaluationsystem.mapper.StudentMapper;
import com.example.graduationevaluationsystem.service.DocumentService;
import com.example.graduationevaluationsystem.service.ScoreRecordService;
import com.example.graduationevaluationsystem.service.StageStatusService;
import com.example.graduationevaluationsystem.service.StudentService;
import com.example.graduationevaluationsystem.vo.StudentAllStatusVO;
import com.example.graduationevaluationsystem.vo.StudentOverallStatusVO;
import com.example.graduationevaluationsystem.vo.StudentTeacherVO;
import com.example.graduationevaluationsystem.vo.TeacherBriefVO;
import com.example.graduationevaluationsystem.vo.StageStatusVO;
import com.example.graduationevaluationsystem.vo.DocumentVO;
import com.example.graduationevaluationsystem.vo.ScoreRecordVO;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 学生 Service 实现
 */
@Service
@RequiredArgsConstructor
public class StudentServiceImpl extends ServiceImpl<StudentMapper, Student> implements StudentService {

    private final StageStatusService stageStatusService;
    private final DocumentService documentService;
    private final ScoreRecordService scoreRecordService;

    @Override
    public int importStudents(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new RuntimeException("文件名不能为空");
        }

        List<Student> students = new ArrayList<>();

        if (filename.toLowerCase().endsWith(".csv")) {
            students = parseCsv(file);
        } else if (filename.toLowerCase().endsWith(".xlsx") || filename.toLowerCase().endsWith(".xls")) {
            students = parseExcel(file);
        } else {
            throw new RuntimeException("仅支持 .xlsx、.xls、.csv 格式文件");
        }

        saveBatch(students);
        return students.size();
    }

    @Override
    public StudentTeacherVO getTeachersByStudentNo(String studentNo) {
        TeacherBriefVO supervisor = baseMapper.selectTeacherByStudentNo(studentNo, "指导");
        TeacherBriefVO reviewer = baseMapper.selectTeacherByStudentNo(studentNo, "评阅");
        StudentTeacherVO vo = new StudentTeacherVO();
        vo.setSupervisor(supervisor);
        vo.setReviewer(reviewer);
        return vo;
    }

    @Override
    public StudentOverallStatusVO getOverallStatus(String studentNo) {
        Student student = getById(studentNo);
        if (student == null) {
            throw new RuntimeException("学生不存在");
        }
        StudentOverallStatusVO vo = new StudentOverallStatusVO();
        vo.setStudentNo(studentNo);
        vo.setOverallStatus(student.getOverallStatus());
        if (student.getOverallStatus() != null) {
            vo.setOverallStatusDesc(OverallStatus.fromCode(student.getOverallStatus()).getDescription());
        }
        return vo;
    }

    @Override
    public void updateOverallStatus(String studentNo, Integer overallStatus) {
        OverallStatus.fromCode(overallStatus);
        Student student = getById(studentNo);
        if (student == null) {
            throw new RuntimeException("学生不存在");
        }
        student.setOverallStatus(overallStatus);
        updateById(student);
    }

    @Override
    public StudentAllStatusVO getAllStatus(String studentNo) {
        Student student = getById(studentNo);
        if (student == null) {
            throw new RuntimeException("学生不存在");
        }

        StudentAllStatusVO vo = new StudentAllStatusVO();
        vo.setStudentNo(studentNo);
        vo.setOverallStatus(student.getOverallStatus());
        if (student.getOverallStatus() != null) {
            vo.setOverallStatusDesc(OverallStatus.fromCode(student.getOverallStatus()).getDescription());
        }

        // 环节状态
        List<StageStatusVO> stageVOs = stageStatusService.getStudentStageStatus(studentNo);
        List<StudentAllStatusVO.StageStatusItem> stageItems = new ArrayList<>();
        for (StageStatusVO stageVO : stageVOs) {
            StudentAllStatusVO.StageStatusItem item = new StudentAllStatusVO.StageStatusItem();
            item.setStage(stageVO.getStage());
            item.setStatus(stageVO.getStatus());
            item.setStatusDesc(stageVO.getStatusDesc());
            stageItems.add(item);
        }
        vo.setStageStatus(stageItems);

        // 文档状态
        List<DocumentVO> docVOs = documentService.getDocsByStudentNo(studentNo, null);
        List<StudentAllStatusVO.DocumentStatusItem> docItems = new ArrayList<>();
        for (DocumentVO docVO : docVOs) {
            StudentAllStatusVO.DocumentStatusItem item = new StudentAllStatusVO.DocumentStatusItem();
            item.setDocType(docVO.getDocType());
            item.setStatus(docVO.getStatus());
            item.setStatusDesc(docVO.getStatusDesc());
            item.setApprovalStatus(docVO.getApprovalStatus());
            item.setApprovalStatusDesc(docVO.getApprovalStatusDesc());
            docItems.add(item);
        }
        vo.setDocumentStatus(docItems);

        // 评价记录状态
        List<ScoreRecordVO> recordVOs = scoreRecordService.getRecordsByStudentNo(studentNo, null);
        List<StudentAllStatusVO.ScoreRecordStatusItem> recordItems = new ArrayList<>();
        for (ScoreRecordVO recordVO : recordVOs) {
            StudentAllStatusVO.ScoreRecordStatusItem item = new StudentAllStatusVO.ScoreRecordStatusItem();
            item.setItemType(recordVO.getItemType());
            item.setRecordStatus(recordVO.getRecordStatus());
            item.setRecordStatusDesc(recordVO.getRecordStatusDesc());
            recordItems.add(item);
        }
        vo.setScoreRecordStatus(recordItems);

        return vo;
    }

    /**
     * 解析 CSV 文件
     */
    private List<Student> parseCsv(MultipartFile file) {
        List<Student> students = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                String[] fields = line.split(",");
                if (fields.length < 3) {
                    continue;
                }
                Student student = new Student();
                student.setStudentNo(fields[0].trim());
                student.setStudentName(fields[1].trim());
                student.setGender(fields.length > 2 ? fields[2].trim() : null);
                student.setClassName(fields.length > 3 ? fields[3].trim() : null);
                student.setMajor(fields.length > 4 ? fields[4].trim() : null);
                student.setGrade(fields.length > 5 ? fields[5].trim() : null);
                student.setStudentGroupId(fields.length > 6 ? Integer.parseInt(fields[6].trim()) : null);
                student.setPassword(fields.length > 7 ? fields[7].trim() : "123456");
                student.setAccountStatus(1);
                student.setOverallStatus(1);
                students.add(student);
            }
        } catch (Exception e) {
            throw new RuntimeException("CSV 文件解析失败: " + e.getMessage());
        }
        return students;
    }

    /**
     * 解析 Excel 文件
     */
    private List<Student> parseExcel(MultipartFile file) {
        List<Student> students = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                Student student = new Student();
                student.setStudentNo(getCellString(row.getCell(0)));
                student.setStudentName(getCellString(row.getCell(1)));
                student.setGender(getCellString(row.getCell(2)));
                student.setClassName(getCellString(row.getCell(3)));
                student.setMajor(getCellString(row.getCell(4)));
                student.setGrade(getCellString(row.getCell(5)));
                String groupIdStr = getCellString(row.getCell(6));
                student.setStudentGroupId(groupIdStr.isEmpty() ? null : Integer.parseInt(groupIdStr));
                String password = getCellString(row.getCell(7));
                student.setPassword(password.isEmpty() ? "123456" : password);
                student.setAccountStatus(1);
                student.setOverallStatus(1);

                if (!student.getStudentNo().isEmpty() && !student.getStudentName().isEmpty()) {
                    students.add(student);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Excel 文件解析失败: " + e.getMessage());
        }
        return students;
    }

    /**
     * 读取单元格内容为字符串
     */
    private String getCellString(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}
