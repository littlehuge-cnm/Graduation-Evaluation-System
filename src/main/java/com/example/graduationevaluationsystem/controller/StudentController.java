package com.example.graduationevaluationsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.graduationevaluationsystem.common.Result;
import com.example.graduationevaluationsystem.entity.Student;
import com.example.graduationevaluationsystem.service.StudentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 学生 Controller
 */
@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
@Tag(name = "学生管理", description = "学生的增删改查及分页查询")
public class StudentController {

    private final StudentService studentService;

    @PostMapping
    public Result<Void> add(@RequestBody Student student) {
        studentService.save(student);
        return Result.success();
    }

    @PutMapping("/{studentNo}")
    public Result<Void> update(@PathVariable String studentNo, @RequestBody Student student) {
        student.setStudentNo(studentNo);
        studentService.updateById(student);
        return Result.success();
    }

    @DeleteMapping("/{studentNo}")
    public Result<Void> delete(@PathVariable String studentNo) {
        studentService.removeById(studentNo);
        return Result.success();
    }

    @GetMapping("/{studentNo}")
    public Result<Student> getById(@PathVariable String studentNo) {
        return Result.success(studentService.getById(studentNo));
    }

    @GetMapping
    public Result<Page<Student>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                      @RequestParam(defaultValue = "10") Integer pageSize,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) Integer groupId) {
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Student::getStudentNo, keyword)
                    .or()
                    .like(Student::getStudentName, keyword);
        }
        if (groupId != null) {
            wrapper.eq(Student::getStudentGroupId, groupId);
        }
        return Result.success(studentService.page(new Page<>(pageNum, pageSize), wrapper));
    }
}
