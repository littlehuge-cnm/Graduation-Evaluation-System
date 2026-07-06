package com.example.graduationevaluationsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.graduationevaluationsystem.common.PageResult;
import com.example.graduationevaluationsystem.common.Result;
import com.example.graduationevaluationsystem.dto.TeacherDTO;
import com.example.graduationevaluationsystem.entity.Teacher;
import com.example.graduationevaluationsystem.service.TeacherService;
import com.example.graduationevaluationsystem.vo.TeacherStudentVO;
import com.example.graduationevaluationsystem.vo.TeacherVO;
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
 * 教师 Controller
 */
@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
@Tag(name = "教师管理", description = "教师的增删改查、分页查询、批量导入及关联学生查询")
public class TeacherController {

    private final TeacherService teacherService;

    @PostMapping
    @Operation(summary = "新增教师", description = "超管新增教师账号")
    public Result<Void> add(@Valid @RequestBody TeacherDTO dto) {
        Teacher teacher = new Teacher();
        BeanUtils.copyProperties(dto, teacher);
        if (teacher.getAccountStatus() == null) {
            teacher.setAccountStatus(1);
        }
        teacherService.save(teacher);
        return Result.success();
    }

    @PutMapping("/{teacherNo}")
    @Operation(summary = "修改教师", description = "超管修改教师信息")
    public Result<Void> update(@Parameter(description = "教师工号") @PathVariable String teacherNo,
                               @RequestBody TeacherDTO dto) {
        Teacher teacher = new Teacher();
        BeanUtils.copyProperties(dto, teacher);
        teacher.setTeacherNo(teacherNo);
        teacherService.updateById(teacher);
        return Result.success();
    }

    @DeleteMapping("/{teacherNo}")
    @Operation(summary = "删除教师", description = "超管删除教师")
    public Result<Void> delete(@Parameter(description = "教师工号") @PathVariable String teacherNo) {
        teacherService.removeById(teacherNo);
        return Result.success();
    }

    @GetMapping("/{teacherNo}")
    @Operation(summary = "查询教师详情", description = "根据工号查询教师详细信息")
    public Result<TeacherVO> getById(@Parameter(description = "教师工号") @PathVariable String teacherNo) {
        Teacher teacher = teacherService.getById(teacherNo);
        if (teacher == null) {
            return Result.error(404, "教师不存在");
        }
        return Result.success(convertToVO(teacher));
    }

    @GetMapping
    @Operation(summary = "分页查询教师列表", description = "支持按工号/姓名模糊查询")
    public Result<PageResult<TeacherVO>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "按工号/姓名模糊查询") @RequestParam(required = false) String keyword) {

        LambdaQueryWrapper<Teacher> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Teacher::getTeacherNo, keyword)
                    .or()
                    .like(Teacher::getTeacherName, keyword);
        }
        wrapper.orderByAsc(Teacher::getTeacherNo);

        Page<Teacher> page = teacherService.page(new Page<>(pageNum, pageSize), wrapper);

        List<TeacherVO> voList = page.getRecords().stream()
                .map(this::convertToVO)
                .toList();

        return Result.success(PageResult.of(page.getTotal(), pageNum, pageSize, voList));
    }

    @PostMapping("/import")
    @Operation(summary = "批量导入教师", description = "通过 Excel/CSV 文件批量导入教师")
    public Result<Integer> importTeachers(@Parameter(description = "Excel/CSV 文件") @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error(400, "文件不能为空");
        }
        int count = teacherService.importTeachers(file);
        return Result.success("成功导入 " + count + " 条记录", count);
    }

    @GetMapping("/{teacherNo}/students")
    @Operation(summary = "按教师查询其学生列表", description = "可根据关系类型过滤（指导/评阅）")
    public Result<List<TeacherStudentVO>> getStudents(
            @Parameter(description = "教师工号") @PathVariable String teacherNo,
            @Parameter(description = "关系类型：指导/评阅") @RequestParam(required = false) String relationType) {

        List<TeacherStudentVO> students = teacherService.getStudentsByTeacherNo(teacherNo, relationType);
        return Result.success(students);
    }

    /**
     * 实体转 VO（隐藏密码，填充状态描述）
     */
    private TeacherVO convertToVO(Teacher teacher) {
        TeacherVO vo = new TeacherVO();
        BeanUtils.copyProperties(teacher, vo);
        Integer status = teacher.getAccountStatus();
        if (status != null) {
            vo.setAccountStatusDesc(status == 1 ? "启用" : status == 2 ? "禁用" : "未知");
        }
        return vo;
    }
}
