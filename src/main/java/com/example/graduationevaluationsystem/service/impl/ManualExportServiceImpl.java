package com.example.graduationevaluationsystem.service.impl;

import com.example.graduationevaluationsystem.entity.Admin;
import com.example.graduationevaluationsystem.entity.GroupMapping;
import com.example.graduationevaluationsystem.entity.Student;
import com.example.graduationevaluationsystem.entity.Teacher;
import com.example.graduationevaluationsystem.service.*;
import com.example.graduationevaluationsystem.vo.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 评价手册导出 Service 实现
 * <p>
 * 直接使用 Word 模板文件，替换 FreeMarker 占位符 ${a}…${bo}，
 * 保证导出文档与模板格式完全一致（分页、字体、表格、边框等）。
 */
@Service
@RequiredArgsConstructor
public class ManualExportServiceImpl implements ManualExportService {

    private final StudentService studentService;
    private final StageStatusService stageStatusService;
    private final DocumentService documentService;
    private final ScoreRecordService scoreRecordService;
    private final StudentGroupService studentGroupService;
    private final TeacherService teacherService;
    private final GroupMappingService groupMappingService;
    private final TeacherGroupService teacherGroupService;
    private final AdminService adminService;

    private static final String CONTENT_SPLIT = "\n\n=====基本要求=====\n\n";
    private static final String TEMPLATE_PATH = "templates/handbook-template.docx";

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
                    if (student == null) continue;
                    byte[] docBytes = generateWordBytes(studentNo);
                    zos.putNextEntry(new ZipEntry(student.getStudentName() + ".docx"));
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
                    if (student == null) continue;
                    byte[] docBytes = generateWordBytes(brief.getStudentNo());
                    zos.putNextEntry(new ZipEntry(student.getStudentName() + ".docx"));
                    zos.write(docBytes);
                    zos.closeEntry();
                }
            }
            response.getOutputStream().flush();
        } catch (IOException e) {
            throw new RuntimeException("按组导出失败: " + e.getMessage());
        }
    }

    // ==================== 核心生成逻辑 ====================

    /**
     * 读取模板 docx（ZIP），替换 document.xml 中的占位符，返回修改后的 docx 字节数组。
     */
    private byte[] generateWordBytes(String studentNo) throws IOException {
        // 收集数据
        Student student = studentService.getById(studentNo);
        StudentTeacherVO teachers = studentService.getTeachersByStudentNo(studentNo);
        List<DocumentVO> documents = documentService.getDocsByStudentNo(studentNo, null);
        List<ScoreRecordVO> records = scoreRecordService.getRecordsByStudentNo(studentNo, null);

        Map<String, ScoreRecordVO> recordMap = new java.util.HashMap<>();
        for (ScoreRecordVO r : records) recordMap.put(r.getItemType(), r);
        Map<String, DocumentVO> docMap = new java.util.HashMap<>();
        for (DocumentVO d : documents) docMap.put(d.getDocType(), d);

        // 构建占位符 -> 值 的映射
        Map<String, String> placeholders = buildPlaceholderMap(student, teachers, docMap, recordMap);

        // 读取模板 ZIP 并替换
        ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH);
        ByteArrayOutputStream result = new ByteArrayOutputStream();

        try (ZipInputStream zis = new ZipInputStream(resource.getInputStream(), StandardCharsets.UTF_8);
             ZipOutputStream zos = new ZipOutputStream(result, StandardCharsets.UTF_8)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                ByteArrayOutputStream entryBuffer = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    entryBuffer.write(buffer, 0, len);
                }
                byte[] entryData = entryBuffer.toByteArray();

                // 只替换 document.xml 中的占位符
                String entryName = entry.getName();
                if ("word/document.xml".equals(entryName)) {
                    String xml = new String(entryData, StandardCharsets.UTF_8);
                    xml = removeUnwantedGuideText(xml);
                    xml = replacePlaceholders(xml, placeholders);
                    entryData = xml.getBytes(StandardCharsets.UTF_8);
                }

                zos.putNextEntry(new ZipEntry(entryName));
                zos.write(entryData);
                zos.closeEntry();
            }
        }
        return result.toByteArray();
    }

    // ==================== 占位符构建 ====================

    @Override
    public Map<String, String> getPreviewData(String studentNo) {
        Student student = studentService.getById(studentNo);
        if (student == null) {
            throw new RuntimeException("学生不存在");
        }
        StudentTeacherVO teachers = studentService.getTeachersByStudentNo(studentNo);
        List<DocumentVO> documents = documentService.getDocsByStudentNo(studentNo, null);
        List<ScoreRecordVO> records = scoreRecordService.getRecordsByStudentNo(studentNo, null);

        Map<String, ScoreRecordVO> recordMap = new java.util.HashMap<>();
        for (ScoreRecordVO r : records) recordMap.put(r.getItemType(), r);
        Map<String, DocumentVO> docMap = new java.util.HashMap<>();
        for (DocumentVO d : documents) docMap.put(d.getDocType(), d);

        return buildPlaceholderMap(student, teachers, docMap, recordMap);
    }

    /**
     * 根据数据库数据，构建模板占位符 -> 实际值 的映射。
     * 占位符定义见 doc/handbook-template-占位符说明(1).md
     */
    private Map<String, String> buildPlaceholderMap(
            Student student,
            StudentTeacherVO teachers,
            Map<String, DocumentVO> docMap,
            Map<String, ScoreRecordVO> recordMap) {

        Map<String, String> m = new java.util.HashMap<>();

        // ---- 封面 ----
        // ${a} 指导教师姓名  ${b} 指导教师职称
        String supervisorName = "";
        String supervisorTitle = "";
        if (teachers != null && teachers.getSupervisor() != null) {
            supervisorName = nvl(teachers.getSupervisor().getTeacherName());
            // 查询 Teacher 实体获取职称
            Teacher supervisor = teacherService.getById(teachers.getSupervisor().getTeacherNo());
            if (supervisor != null) {
                supervisorTitle = nvl(supervisor.getTitle());
            }
        }
        m.put("a", supervisorName);
        m.put("b", supervisorTitle);

        // ${c} 学生姓名  ${d} 专业  ${e} 班级  ${f} 学号
        m.put("c", student != null ? nvl(student.getStudentName()) : "");
        m.put("d", student != null ? nvl(student.getMajor()) : "");
        m.put("e", student != null ? nvl(student.getClassName()) : "");
        m.put("f", student != null ? nvl(student.getStudentNo()) : "");

        // ---- 任务书 ----
        DocumentVO taskDoc = docMap.get("任务书");
        // ${g} 题目
        m.put("g", taskDoc != null ? nvl(taskDoc.getTitle()) : "");
        // ${h} 课题类别  ${i} 课题类型  ${j} 新旧课题
        m.put("h", taskDoc != null ? nvl(taskDoc.getSubjectCategory()) : "");
        m.put("i", taskDoc != null ? nvl(taskDoc.getSubjectType()) : "");
        m.put("j", taskDoc != null ? nvl(taskDoc.getSubjectNewOld()) : "");
        // ${k} 课题研究主要内容  ${l} 基本要求
        String mainContent = "";
        String basicRequirement = "";
        if (taskDoc != null && taskDoc.getContent() != null) {
            String content = taskDoc.getContent();
            int idx = content.indexOf(CONTENT_SPLIT);
            if (idx >= 0) {
                mainContent = content.substring(0, idx);
                basicRequirement = content.substring(idx + CONTENT_SPLIT.length());
            } else {
                mainContent = content;
            }
        }
        m.put("k", mainContent);
        m.put("l", basicRequirement);
        // ${m}~${z} 各种签字/意见 — 留空
        for (char ch = 'm'; ch <= 'z'; ch++) {
            m.put(String.valueOf(ch), "");
        }

        // 获取超管姓名（用于系主任签字、院长签字等）
        String adminName = "";
        Admin admin = adminService.lambdaQuery()
            .eq(Admin::getAccountStatus, 1)
            .last("LIMIT 1")
            .one();
        if (admin != null) {
            adminName = nvl(admin.getAdminName());
        }

        // ${m} 导师签字 = 指导教师姓名
        m.put("m", supervisorName);
        // ${r} 系主任签字 = 超管姓名
        m.put("r", adminName);
        // ${w} 院长签字 = 超管姓名
        m.put("w", adminName);

        // ---- 指导书 ----
        DocumentVO guideDoc = docMap.get("指导书");
        String guideContent = guideDoc != null ? nvl(guideDoc.getContent()) : "";
        // ${A} 课题研究主要目的和要求 — 放全部内容
        m.put("A", guideContent);
        // ${B}~${G} 补充要求 — 留空
        for (char ch = 'B'; ch <= 'G'; ch++) {
            m.put(String.valueOf(ch), "");
        }

        // ---- 开题报告成绩 ----
        ScoreRecordVO openingRecord = recordMap.get("开题报告成绩");
        // ${H} 开题报告总成绩
        m.put("H", getScore(openingRecord));

        // ---- 外文翻译成绩 ----
        ScoreRecordVO translationRecord = recordMap.get("外文翻译");
        int[] trSubScores = parseSubScores(translationRecord);
        // ${I} 阅读理解  ${J} 专业词语  ${K} 译文规范性  ${L} 总成绩
        m.put("I", getSubScore(trSubScores, 0));
        m.put("J", getSubScore(trSubScores, 1));
        m.put("K", getSubScore(trSubScores, 2));
        m.put("L", getScore(translationRecord));

        // ---- 中期检查 ----
        ScoreRecordVO midtermRecord = recordMap.get("中期检查成绩");
        // ${M} 总成绩  ${N} 评语  ${O} 组长签字  ${P}~${R} 年月日
        m.put("M", getScore(midtermRecord));
        m.put("N", getComment(midtermRecord));
        m.put("O", getRecorderName(midtermRecord));
        setDatePlaceholders(m, midtermRecord, "P", "Q", "R");

        // ---- 指导老师评语 ----
        ScoreRecordVO advisorRecord = recordMap.get("指导评语");
        int[] adSubScores = parseSubScores(advisorRecord);
        // ${S}~${W} 五项分项成绩  ${X} 合计  ${Y} 评语  ${Z} 签字  ${aa}~${ac} 年月日
        m.put("S", getSubScore(adSubScores, 0));
        m.put("T", getSubScore(adSubScores, 1));
        m.put("U", getSubScore(adSubScores, 2));
        m.put("V", getSubScore(adSubScores, 3));
        m.put("W", getSubScore(adSubScores, 4));
        m.put("X", getScore(advisorRecord));
        m.put("Y", getComment(advisorRecord));
        m.put("Z", getRecorderName(advisorRecord));
        setDatePlaceholders(m, advisorRecord, "aa", "ab", "ac");

        // ---- 评阅教师评分 ----
        ScoreRecordVO reviewerRecord = recordMap.get("评阅评语");
        int[] rvSubScores = parseSubScores(reviewerRecord);
        // ${ad}~${ag} 四项分项成绩  ${ah} 合计  ${ai} 评语  ${aj} 签字  ${ak}~${am} 年月日
        m.put("ad", getSubScore(rvSubScores, 0));
        m.put("ae", getSubScore(rvSubScores, 1));
        m.put("af", getSubScore(rvSubScores, 2));
        m.put("ag", getSubScore(rvSubScores, 3));
        m.put("ah", getScore(reviewerRecord));
        m.put("ai", getComment(reviewerRecord));
        // ${aj} 评阅教师签字 - 优先使用评阅教师姓名，其次用录入人
        String reviewerName = "";
        if (teachers != null && teachers.getReviewer() != null) {
            reviewerName = nvl(teachers.getReviewer().getTeacherName());
        }
        if (reviewerName.isEmpty()) {
            reviewerName = getRecorderName(reviewerRecord);
        }
        m.put("aj", reviewerName);
        setDatePlaceholders(m, reviewerRecord, "ak", "al", "am");

        // ---- 答辩记录 ----
        ScoreRecordVO defenseRecordRecord = recordMap.get("答辩记录");

        // 提前查询答辩小组教师信息（用于答辩记录和答辩成绩两部分）
        String defenseLeaderName = "";
        String defenseSecretaryName = "";
        String defenseMemberNames = "";
        if (student != null && student.getStudentGroupId() != null) {
            GroupMapping mapping = groupMappingService.lambdaQuery()
                .eq(GroupMapping::getStage, "答辩")
                .eq(GroupMapping::getStudentGroupId, student.getStudentGroupId())
                .one();
            if (mapping != null && mapping.getTeacherGroupId() != null) {
                TeacherGroupVO tg = teacherGroupService.getGroupById(mapping.getTeacherGroupId());
                if (tg != null) {
                    defenseLeaderName = nvl(tg.getLeaderName());
                    defenseSecretaryName = nvl(tg.getSecretaryName());
                    StringBuilder members = new StringBuilder();
                    if (tg.getSecretaryName() != null && !tg.getSecretaryName().isEmpty()) {
                        members.append(tg.getSecretaryName());
                    }
                    if (tg.getMemberName() != null && !tg.getMemberName().isEmpty()) {
                        if (members.length() > 0) members.append("、");
                        members.append(tg.getMemberName());
                    }
                    defenseMemberNames = members.toString();
                }
            }
        }

        // ${an} 答辩日期  ${ao} 学生姓名  ${ap} 记录人  ${aq} 记录内容
        m.put("an", getRecordTime(defenseRecordRecord));
        m.put("ao", student != null ? nvl(student.getStudentName()) : "");
        // ${ap} 记录人 - 优先用录入人，其次用答辩小组秘书
        String recorderName = getRecorderName(defenseRecordRecord);
        if (recorderName.isEmpty()) {
            recorderName = defenseSecretaryName;
        }
        m.put("ap", recorderName);
        String defenseContent = "";
        if (defenseRecordRecord != null) {
            defenseContent = nvl(defenseRecordRecord.getDefenseRecord());
            if (defenseContent.isEmpty()) {
                defenseContent = nvl(defenseRecordRecord.getComment());
            }
        }
        m.put("aq", defenseContent);

        // ---- 答辩小组评定成绩 ----
        ScoreRecordVO defenseScoreRecord = recordMap.get("毕业答辩成绩");
        // ${ar} 答辩成绩  ${as} 评语  ${at} 组长签字  ${au} 成员签字  ${av}~${ax} 年月日
        m.put("ar", getScore(defenseScoreRecord));
        m.put("as", getComment(defenseScoreRecord));
        m.put("at", defenseLeaderName);
        m.put("au", defenseMemberNames);
        setDatePlaceholders(m, defenseScoreRecord, "av", "aw", "ax");

        // ---- 答辩委员会总评 ----
        ScoreRecordVO committeeRecord = recordMap.get("委员会评定");
        // ${ay}~${bj} 各项得分和备注
        m.put("ay", getScore(recordMap.get("开题报告成绩")));
        m.put("az", "");
        m.put("ba", getScore(recordMap.get("外文翻译")));
        m.put("bb", "");
        m.put("bc", getScore(recordMap.get("中期检查成绩")));
        m.put("bd", "");
        m.put("be", getScore(recordMap.get("指导评语")));
        m.put("bf", "");
        m.put("bg", getScore(recordMap.get("评阅评语")));
        m.put("bh", "");
        m.put("bi", getScore(recordMap.get("毕业答辩成绩")));
        m.put("bj", "");
        // ${bk} 总分  ${bl} 等级  ${bm} 评语  ${bn} 主任签字  ${bo} 秘书签字
        m.put("bk", getScore(committeeRecord));
        m.put("bl", committeeRecord != null ? nvl(committeeRecord.getGrade()) : "");
        m.put("bm", getComment(committeeRecord));
        m.put("bn", adminName);
        m.put("bo", adminName);

        return m;
    }

    // ==================== 指导书固定文字删除 ====================

    /**
     * 删除指导书中"二、实施安排"到"七、其他"之间的固定描述文字。
     * 保留 ${B}~${G} 占位符，仅清除固定模板文字。
     */
    private String removeUnwantedGuideText(String xml) {
        // 定位起始：包含"二、实施安排"的 <w:t> 元素
        String startMarker = "\u4e8c\u3001\u5b9e\u65bd\u5b89\u6392";
        int startIdx = xml.indexOf(startMarker);
        if (startIdx < 0) return xml;
        // 向前找到 <w:t> 或 <w:t (带属性) 标签开头，不能匹配 <w:tabs> 等
        int wt1 = xml.lastIndexOf("<w:t>", startIdx);
        int wt2 = xml.lastIndexOf("<w:t ", startIdx);
        int wtStart = Math.max(wt1, wt2);
        if (wtStart < 0) return xml;

        // 定位结束：包含 "${G}" 的 </w:t> 标签之后
        String endMarker = "${G}";
        int endIdx = xml.indexOf(endMarker, startIdx);
        if (endIdx < 0) return xml;
        int wtEnd = xml.indexOf("</w:t>", endIdx);
        if (wtEnd < 0) return xml;
        wtEnd += "</w:t>".length();

        // 提取要处理的区段
        String section = xml.substring(wtStart, wtEnd);

        // 逐个处理 <w:t> 元素：含占位符的只保留占位符，其余清空文字
        // 注意：正则只匹配 <w:t> 或 <w:t 属性...>，不匹配 <w:tabs> 等
        java.util.regex.Pattern wtPattern = java.util.regex.Pattern.compile(
            "(<w:t(?:\\s[^>]*)??>)(.*?)(</w:t>)", java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher m = wtPattern.matcher(section);
        StringBuffer result = new StringBuffer();
        java.util.regex.Pattern placeholderPattern = java.util.regex.Pattern.compile(
            "\\$\\{[B-G]\\}");
        while (m.find()) {
            String openTag = m.group(1);
            String text = m.group(2);
            String closeTag = m.group(3);
            java.util.regex.Matcher pm = placeholderPattern.matcher(text);
            if (pm.find()) {
                // 保留占位符，删除其余文字
                m.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(
                    openTag + pm.group() + closeTag));
            } else {
                // 清空文字
                m.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(
                    openTag + closeTag));
            }
        }
        m.appendTail(result);

        // 清除文字后，删除空段落（没有文字内容的 <w:p>）以消除空白
        String cleared = result.toString();
        java.util.regex.Pattern pPattern = java.util.regex.Pattern.compile(
            "<w:p[^>]*>.*?</w:p>", java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher pm = pPattern.matcher(cleared);
        StringBuffer pr = new StringBuffer();
        java.util.regex.Pattern nonEmptyText = java.util.regex.Pattern.compile(
            "<w:t(?:\\s[^>]*)??>[^<]+</w:t>");
        while (pm.find()) {
            String para = pm.group();
            if (nonEmptyText.matcher(para).find()) {
                // 段落有文字内容，保留
                pm.appendReplacement(pr, java.util.regex.Matcher.quoteReplacement(para));
            } else {
                // 空段落，删除
                pm.appendReplacement(pr, "");
            }
        }
        pm.appendTail(pr);

        return xml.substring(0, wtStart) + pr.toString() + xml.substring(wtEnd);
    }

    // ==================== 占位符替换 ====================

    /**
     * 在 document.xml 字符串中替换所有 ${xx} 占位符。
     * 需要注意：占位符可能被 XML 标签分割在多个 w:t 元素中，
     * 因此先移除 w:t 之间的标签，替换后再还原。
     * <p>
     * 更安全的做法：直接在整个 XML 字符串上做正则替换，
     * 因为模板已经 merge runs，占位符不会跨 run。
     */
    private String replacePlaceholders(String xml, Map<String, String> placeholders) {
        // 预处理：修复被 XML 标签分割的占位符
        // 例如: ${a</w:t><w:t>j} 会被修复为 ${aj}
        xml = fixSplitPlaceholders(xml);

        // 按占位符 key 长度降序排序，避免 ${a} 先匹配到 ${aa} 的子串
        List<String> sortedKeys = new java.util.ArrayList<>(placeholders.keySet());
        sortedKeys.sort((a, b) -> b.length() - a.length());

        for (String key : sortedKeys) {
            String value = placeholders.get(key);
            if (value == null) value = "";
            // XML 转义
            value = xmlEscape(value);
            // 将换行符转换为 Word 换行标签，使内容不在同一行
            value = value.replace("\r\n", "\n").replace("\r", "\n");
            value = value.replace("\n", "</w:t><w:br/><w:t xml:space=\"preserve\">");
            // 替换 ${key}，注意 $ 在正则中需要转义
            String pattern = "\\$\\{" + java.util.regex.Pattern.quote(key) + "\\}";
            xml = xml.replaceAll(pattern, java.util.regex.Matcher.quoteReplacement(value));
        }
        return xml;
    }

    /**
     * 修复被 XML 标签分割的占位符。
     * <p>
     * Word XML 中，占位符如 ${aj} 可能被分割在多个 <w:t> 元素中：
     * <w:t>${a</w:t></w:r><w:r><w:t>j}</w:t>
     * <p>
     * 此方法查找所有 ${...} 模式（即使跨越 XML 标签），
     * 去除其中的 XML 标签得到纯 key，然后用干净的 ${key} 替换。
     */
    private String fixSplitPlaceholders(String xml) {
        java.util.regex.Pattern splitFix = java.util.regex.Pattern.compile(
            "\\$\\{(.*?)\\}", java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher m = splitFix.matcher(xml);
        StringBuffer fixed = new StringBuffer();
        while (m.find()) {
            String rawContent = m.group(1);
            // 去除 XML 标签，得到纯占位符 key
            String cleanKey = rawContent.replaceAll("<[^>]*>", "");
            m.appendReplacement(fixed, java.util.regex.Matcher.quoteReplacement("${" + cleanKey + "}"));
        }
        m.appendTail(fixed);
        return fixed.toString();
    }

    // ==================== 辅助方法 ====================

    /**
     * XML 文本节点转义
     */
    private String xmlEscape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;");
    }

    /**
     * null 转空字符串
     */
    private String nvl(String s) {
        return s != null ? s : "";
    }

    /**
     * 解析子成绩字符串（逗号分隔）
     */
    private int[] parseSubScores(ScoreRecordVO record) {
        if (record == null || record.getSubScores() == null || record.getSubScores().isEmpty()) {
            return new int[0];
        }
        String[] parts = record.getSubScores().split(",");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                result[i] = Integer.parseInt(parts[i].trim());
            } catch (NumberFormatException e) {
                result[i] = 0;
            }
        }
        return result;
    }

    /**
     * 获取子成绩字符串
     */
    private String getSubScore(int[] subScores, int index) {
        if (index < subScores.length) {
            return String.valueOf(subScores[index]);
        }
        return "";
    }

    /**
     * 获取总成绩
     */
    private String getScore(ScoreRecordVO record) {
        if (record != null && record.getScore() != null) {
            String s = record.getScore().toPlainString();
            // 去掉多余小数点，如 9.00 -> 9, 9.50 -> 9.5
            if (s.contains(".")) {
                s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
            }
            return s;
        }
        return "";
    }

    /**
     * 获取评语
     */
    private String getComment(ScoreRecordVO record) {
        if (record != null && record.getComment() != null) {
            return record.getComment();
        }
        return "";
    }

    /**
     * 获取录入人姓名
     */
    private String getRecorderName(ScoreRecordVO record) {
        if (record != null && record.getRecorderName() != null) {
            return record.getRecorderName();
        }
        return "";
    }

    /**
     * 获取录入时间格式化字符串
     */
    private String getRecordTime(ScoreRecordVO record) {
        if (record != null && record.getRecordTime() != null) {
            return record.getRecordTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        return "";
    }

    /**
     * 将 recordTime 拆分为年、月、日，放入占位符 map
     */
    private void setDatePlaceholders(Map<String, String> m, ScoreRecordVO record,
                                     String yearKey, String monthKey, String dayKey) {
        if (record != null && record.getRecordTime() != null) {
            LocalDateTime dt = record.getRecordTime();
            m.put(yearKey, String.valueOf(dt.getYear()));
            m.put(monthKey, String.valueOf(dt.getMonthValue()));
            m.put(dayKey, String.valueOf(dt.getDayOfMonth()));
        } else {
            m.put(yearKey, "");
            m.put(monthKey, "");
            m.put(dayKey, "");
        }
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
