package com.example.graduationevaluationsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.graduationevaluationsystem.common.PageResult;
import com.example.graduationevaluationsystem.common.Result;
import com.example.graduationevaluationsystem.dto.OverallStatusDTO;
import com.example.graduationevaluationsystem.dto.StudentDTO;
import com.example.graduationevaluationsystem.entity.Student;
import com.example.graduationevaluationsystem.service.DocumentService;
import com.example.graduationevaluationsystem.service.ScoreRecordService;
import com.example.graduationevaluationsystem.service.StudentService;
import com.example.graduationevaluationsystem.vo.DocumentVO;
import com.example.graduationevaluationsystem.vo.ScoreRecordVO;
import com.example.graduationevaluationsystem.vo.StudentAllStatusVO;
import com.example.graduationevaluationsystem.vo.StudentOverallStatusVO;
import com.example.graduationevaluationsystem.vo.StudentTeacherVO;
import com.example.graduationevaluationsystem.vo.StudentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 学生 Controller
 */
@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
@Tag(name = "学生管理", description = "学生的增删改查、分页查询、批量导入及关联教师查询")
public class StudentController {

    private final StudentService studentService;
    private final DocumentService documentService;
    private final ScoreRecordService scoreRecordService;

    @PostMapping
    @Operation(summary = "新增学生", description = "超管新增学生账号")
    public Result<Void> add(@Valid @RequestBody StudentDTO dto) {
        Student student = new Student();
        BeanUtils.copyProperties(dto, student);
        if (student.getAccountStatus() == null) {
            student.setAccountStatus(1);
        }
        if (student.getOverallStatus() == null) {
            student.setOverallStatus(1);
        }
        if (student.getStudentGroupId() == null) {
            student.setStudentGroupId(null);
        }
        studentService.save(student);
        return Result.success();
    }

    @PutMapping("/{studentNo}")
    @Operation(summary = "修改学生", description = "超管修改学生信息")
    public Result<Void> update(@Parameter(description = "学号") @PathVariable String studentNo,
                               @RequestBody StudentDTO dto) {
        Student student = new Student();
        BeanUtils.copyProperties(dto, student);
        student.setStudentNo(studentNo);
        studentService.updateById(student);
        return Result.success();
    }

    @DeleteMapping("/{studentNo}")
    @Operation(summary = "删除学生", description = "超管删除学生")
    public Result<Void> delete(@Parameter(description = "学号") @PathVariable String studentNo) {
        studentService.removeById(studentNo);
        return Result.success();
    }

    @GetMapping("/{studentNo}")
    @Operation(summary = "查询学生详情", description = "根据学号查询学生详细信息")
    public Result<StudentVO> getById(@Parameter(description = "学号") @PathVariable String studentNo) {
        Student student = studentService.getById(studentNo);
        if (student == null) {
            return Result.error(404, "学生不存在");
        }
        return Result.success(convertToVO(student));
    }

    @GetMapping
    @Operation(summary = "分页查询学生列表", description = "支持按学号/姓名模糊查询、按学生组号过滤")
    public Result<PageResult<StudentVO>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "按学号/姓名模糊查询") @RequestParam(required = false) String keyword,
            @Parameter(description = "按学生组号过滤") @RequestParam(required = false) Integer groupId) {

        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Student::getStudentNo, keyword)
                    .or()
                    .like(Student::getStudentName, keyword);
        }
        if (groupId != null) {
            wrapper.eq(Student::getStudentGroupId, groupId);
        }
        wrapper.orderByAsc(Student::getStudentNo);

        Page<Student> page = studentService.page(new Page<>(pageNum, pageSize), wrapper);

        List<StudentVO> voList = page.getRecords().stream()
                .map(this::convertToVO)
                .toList();

        return Result.success(PageResult.of(page.getTotal(), pageNum, pageSize, voList));
    }

    @PostMapping("/import")
    @Operation(summary = "批量导入学生", description = "通过 Excel/CSV 文件批量导入学生")
    public Result<Integer> importStudents(@Parameter(description = "Excel/CSV 文件") @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error(400, "文件不能为空");
        }
        int count = studentService.importStudents(file);
        return Result.success("成功导入 " + count + " 条记录", count);
    }

    @GetMapping("/{studentNo}/teachers")
    @Operation(summary = "按学生查询其指导/评阅教师", description = "返回该学生的指导教师和评阅教师信息")
    public Result<StudentTeacherVO> getTeachers(
            @Parameter(description = "学号") @PathVariable String studentNo) {
        StudentTeacherVO vo = studentService.getTeachersByStudentNo(studentNo);
        return Result.success(vo);
    }

    @GetMapping("/{studentNo}/documents")
    @Operation(summary = "按学生查询文档列表", description = "返回该学生的任务书/指导书列表，可按文档类型过滤")
    public Result<List<DocumentVO>> getDocuments(
            @Parameter(description = "学号") @PathVariable String studentNo,
            @Parameter(description = "文档类型：任务书/指导书") @RequestParam(required = false) String docType) {
        return Result.success(documentService.getDocsByStudentNo(studentNo, docType));
    }

    @GetMapping("/{studentNo}/score-records")
    @Operation(summary = "按学生查询评价记录", description = "返回该学生的全部评价记录，可按条目类型过滤")
    public Result<List<ScoreRecordVO>> getScoreRecords(
            @Parameter(description = "学号") @PathVariable String studentNo,
            @Parameter(description = "条目类型：如开题成绩/外文翻译/中期检查等") @RequestParam(required = false) String itemType) {
        return Result.success(scoreRecordService.getRecordsByStudentNo(studentNo, itemType));
    }

    @GetMapping("/{studentNo}/overall-status")
    @Operation(summary = "查询学生整体进度", description = "返回学生整体进度状态及中文描述")
    public Result<StudentOverallStatusVO> getOverallStatus(
            @Parameter(description = "学号") @PathVariable String studentNo) {
        return Result.success(studentService.getOverallStatus(studentNo));
    }

    @PutMapping("/{studentNo}/overall-status")
    @Operation(summary = "标记学生进度状态", description = "超管手动标记学生整体进度（3=待答辩/5=已弃做）")
    public Result<Void> updateOverallStatus(
            @Parameter(description = "学号") @PathVariable String studentNo,
            @Valid @RequestBody OverallStatusDTO dto) {
        studentService.updateOverallStatus(studentNo, dto.getOverallStatus());
        return Result.success();
    }

    @GetMapping("/{studentNo}/all-status")
    @Operation(summary = "查询学生全部状态", description = "一次返回学生的整体进度、各环节状态、文档状态、评价记录状态")
    public Result<StudentAllStatusVO> getAllStatus(
            @Parameter(description = "学号") @PathVariable String studentNo) {
        return Result.success(studentService.getAllStatus(studentNo));
    }

    /**
     * 实体转 VO（隐藏密码，填充状态描述）
     */
    private StudentVO convertToVO(Student student) {
        StudentVO vo = new StudentVO();
        BeanUtils.copyProperties(student, vo);
        Integer accountStatus = student.getAccountStatus();
        if (accountStatus != null) {
            vo.setAccountStatusDesc(accountStatus == 1 ? "启用" : accountStatus == 2 ? "禁用" : "未知");
        }
        Integer overallStatus = student.getOverallStatus();
        if (overallStatus != null) {
            vo.setOverallStatusDesc(switch (overallStatus) {
                case 1 -> "待分配";
                case 2 -> "进行中";
                case 3 -> "待答辩";
                case 4 -> "已完成";
                case 5 -> "已弃做";
                default -> "未知";
            });
        }
        return vo;
    }
}
