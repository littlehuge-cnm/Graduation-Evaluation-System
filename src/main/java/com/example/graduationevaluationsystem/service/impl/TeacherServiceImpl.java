package com.example.graduationevaluationsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.graduationevaluationsystem.entity.Teacher;
import com.example.graduationevaluationsystem.mapper.TeacherMapper;
import com.example.graduationevaluationsystem.service.TeacherService;
import com.example.graduationevaluationsystem.vo.TeacherStudentVO;
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
 * 教师 Service 实现
 */
@Service
@RequiredArgsConstructor
public class TeacherServiceImpl extends ServiceImpl<TeacherMapper, Teacher> implements TeacherService {

    @Override
    public int importTeachers(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new RuntimeException("文件名不能为空");
        }

        List<Teacher> teachers = new ArrayList<>();

        if (filename.toLowerCase().endsWith(".csv")) {
            teachers = parseCsv(file);
        } else if (filename.toLowerCase().endsWith(".xlsx") || filename.toLowerCase().endsWith(".xls")) {
            teachers = parseExcel(file);
        } else {
            throw new RuntimeException("仅支持 .xlsx、.xls、.csv 格式文件");
        }

        // 批量保存
        saveBatch(teachers);
        return teachers.size();
    }

    @Override
    public List<TeacherStudentVO> getStudentsByTeacherNo(String teacherNo, String relationType) {
        List<TeacherStudentVO> list = baseMapper.selectStudentsByTeacherNo(teacherNo, relationType);
        list.forEach(this::fillRelationStatusDesc);
        return list;
    }

    private void fillRelationStatusDesc(TeacherStudentVO vo) {
        Integer status = vo.getRelationStatus();
        if (status != null) {
            vo.setRelationStatusDesc(switch (status) {
                case 1 -> "生效";
                case 2 -> "已解除";
                default -> "未知";
            });
        }
    }

    /**
     * 解析 CSV 文件
     */
    private List<Teacher> parseCsv(MultipartFile file) {
        List<Teacher> teachers = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    // 跳过表头
                    continue;
                }
                String[] fields = line.split(",");
                if (fields.length < 2) {
                    continue;
                }
                Teacher teacher = new Teacher();
                teacher.setTeacherNo(fields[0].trim());
                teacher.setTeacherName(fields[1].trim());
                teacher.setGender(fields.length > 2 ? fields[2].trim() : null);
                teacher.setDepartment(fields.length > 3 ? fields[3].trim() : null);
                teacher.setTitle(fields.length > 4 ? fields[4].trim() : null);
                teacher.setPhone(fields.length > 5 ? fields[5].trim() : null);
                teacher.setPassword(fields.length > 6 ? fields[6].trim() : "123456");
                teacher.setAccountStatus(1);
                teachers.add(teacher);
            }
        } catch (Exception e) {
            throw new RuntimeException("CSV 文件解析失败: " + e.getMessage());
        }
        return teachers;
    }

    /**
     * 解析 Excel 文件
     */
    private List<Teacher> parseExcel(MultipartFile file) {
        List<Teacher> teachers = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                Teacher teacher = new Teacher();
                teacher.setTeacherNo(getCellString(row.getCell(0)));
                teacher.setTeacherName(getCellString(row.getCell(1)));
                teacher.setGender(getCellString(row.getCell(2)));
                teacher.setDepartment(getCellString(row.getCell(3)));
                teacher.setTitle(getCellString(row.getCell(4)));
                teacher.setPhone(getCellString(row.getCell(5)));
                String password = getCellString(row.getCell(6));
                teacher.setPassword(password.isEmpty() ? "123456" : password);
                teacher.setAccountStatus(1);

                if (!teacher.getTeacherNo().isEmpty() && !teacher.getTeacherName().isEmpty()) {
                    teachers.add(teacher);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Excel 文件解析失败: " + e.getMessage());
        }
        return teachers;
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
