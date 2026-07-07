package com.example.graduationevaluationsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.graduationevaluationsystem.entity.Student;
import com.example.graduationevaluationsystem.vo.StudentAllStatusVO;
import com.example.graduationevaluationsystem.vo.StudentOverallStatusVO;
import com.example.graduationevaluationsystem.vo.StudentTeacherVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 学生 Service
 */
public interface StudentService extends IService<Student> {

    /**
     * 批量导入学生
     *
     * @param file Excel/CSV 文件
     * @return 导入成功的数量
     */
    int importStudents(MultipartFile file);

    /**
     * 按学号查询其指导/评阅教师
     *
     * @param studentNo 学号
     * @return 指导教师与评阅教师信息
     */
    StudentTeacherVO getTeachersByStudentNo(String studentNo);

    /**
     * 查询学生整体进度状态
     *
     * @param studentNo 学号
     * @return 整体进度状态
     */
    StudentOverallStatusVO getOverallStatus(String studentNo);

    /**
     * 修改学生整体进度状态（超管操作）
     *
     * @param studentNo      学号
     * @param overallStatus  整体进度状态码
     */
    void updateOverallStatus(String studentNo, Integer overallStatus);

    /**
     * 查询学生全部状态（整体进度、各环节状态、文档状态、评价记录状态）
     *
     * @param studentNo 学号
     * @return 全部状态
     */
    StudentAllStatusVO getAllStatus(String studentNo);
}
