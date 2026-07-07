package com.example.graduationevaluationsystem.controller;

import com.example.graduationevaluationsystem.common.Result;
import com.example.graduationevaluationsystem.dto.RelationStatusDTO;
import com.example.graduationevaluationsystem.dto.TeacherStudentDTO;
import com.example.graduationevaluationsystem.entity.TeacherStudent;
import com.example.graduationevaluationsystem.service.TeacherStudentService;
import com.example.graduationevaluationsystem.vo.TeacherStudentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 师生关系 Controller
 */
@RestController
@RequestMapping("/api/teacher-students")
@RequiredArgsConstructor
@Tag(name = "师生关系管理", description = "指导与评阅师生关系的指定、修改、删除及解除")
public class TeacherStudentController {

    private final TeacherStudentService teacherStudentService;

    @PostMapping
    @Operation(summary = "指定指导/评阅教师", description = "超管为学生指定指导教师或评阅教师，指定指导教师后学生进度自动变为进行中")
    public Result<Void> add(@Valid @RequestBody TeacherStudentDTO dto) {
        TeacherStudent teacherStudent = new TeacherStudent();
        BeanUtils.copyProperties(dto, teacherStudent);
        teacherStudent.setRelationStatus(1);
        teacherStudentService.save(teacherStudent);
        return Result.success();
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改师生关系", description = "超管修改师生关系记录")
    public Result<Void> update(@Parameter(description = "记录编号") @PathVariable Integer id,
                               @RequestBody TeacherStudentDTO dto) {
        TeacherStudent teacherStudent = new TeacherStudent();
        BeanUtils.copyProperties(dto, teacherStudent);
        teacherStudent.setId(id);
        teacherStudentService.updateById(teacherStudent);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除师生关系", description = "超管删除师生关系记录")
    public Result<Void> delete(@Parameter(description = "记录编号") @PathVariable Integer id) {
        teacherStudentService.removeById(id);
        return Result.success();
    }

    @GetMapping
    @Operation(summary = "查询师生关系列表", description = "支持按教师工号、学号、关系类型过滤")
    public Result<List<TeacherStudentVO>> list(
            @Parameter(description = "教师工号") @RequestParam(required = false) String teacherNo,
            @Parameter(description = "学号") @RequestParam(required = false) String studentNo,
            @Parameter(description = "关系类型：指导/评阅") @RequestParam(required = false) String relationType) {

        List<TeacherStudentVO> list = teacherStudentService.getRelationList(teacherNo, studentNo, relationType);
        return Result.success(list);
    }

    @PutMapping("/{id}/relation-status")
    @Operation(summary = "解除师生关系", description = "超管解除师生关系，状态从1（生效）变为2（已解除），历史成绩评语保留")
    public Result<Void> updateRelationStatus(
            @Parameter(description = "记录编号") @PathVariable Integer id,
            @Valid @RequestBody RelationStatusDTO dto) {
        teacherStudentService.updateRelationStatus(id, dto.getRelationStatus());
        return Result.success();
    }
}
