package com.example.graduationevaluationsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.graduationevaluationsystem.common.Result;
import com.example.graduationevaluationsystem.entity.TeacherStudent;
import com.example.graduationevaluationsystem.service.TeacherStudentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 师生关系 Controller
 */
@RestController
@RequestMapping("/api/teacher-students")
@RequiredArgsConstructor
@Tag(name = "师生关系管理", description = "指导与评阅师生关系的管理")
public class TeacherStudentController {

    private final TeacherStudentService teacherStudentService;

    @PostMapping
    public Result<Void> add(@RequestBody TeacherStudent teacherStudent) {
        teacherStudentService.save(teacherStudent);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Integer id, @RequestBody TeacherStudent teacherStudent) {
        teacherStudent.setId(id);
        teacherStudentService.updateById(teacherStudent);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        teacherStudentService.removeById(id);
        return Result.success();
    }

    @GetMapping
    public Result<List<TeacherStudent>> list(@RequestParam(required = false) String teacherNo,
                                             @RequestParam(required = false) String studentNo,
                                             @RequestParam(required = false) String relationType) {
        LambdaQueryWrapper<TeacherStudent> wrapper = new LambdaQueryWrapper<>();
        if (teacherNo != null) {
            wrapper.eq(TeacherStudent::getTeacherNo, teacherNo);
        }
        if (studentNo != null) {
            wrapper.eq(TeacherStudent::getStudentNo, studentNo);
        }
        if (relationType != null) {
            wrapper.eq(TeacherStudent::getRelationType, relationType);
        }
        return Result.success(teacherStudentService.list(wrapper));
    }
}
