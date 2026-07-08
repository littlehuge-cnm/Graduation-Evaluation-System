-- =====================================================================
-- 本科生毕业设计评价系统 数据库初始化脚本
-- 数据库：graduation_evaluation
-- 字符集：utf8mb4
-- 引擎：InnoDB
-- =====================================================================

CREATE DATABASE IF NOT EXISTS graduation_evaluation
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE graduation_evaluation;

-- ---------------------------------------------------------------------
-- 1. 超级管理员表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS t_admin;
CREATE TABLE t_admin (
    admin_id        VARCHAR(20)  NOT NULL COMMENT '管理员账号',
    admin_name      VARCHAR(50)  NOT NULL COMMENT '姓名',
    password        VARCHAR(100) NOT NULL COMMENT '登录密码',
    account_status  VARCHAR(10)  NOT NULL DEFAULT '启用' COMMENT '账号状态（启用/禁用）',
    PRIMARY KEY (admin_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '超级管理员表';

-- ---------------------------------------------------------------------
-- 2. 教师表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS t_teacher;
CREATE TABLE t_teacher (
    teacher_no      VARCHAR(20)  NOT NULL COMMENT '工号',
    teacher_name    VARCHAR(50)  NOT NULL COMMENT '姓名',
    gender          VARCHAR(4)   DEFAULT NULL COMMENT '性别（男/女）',
    department      VARCHAR(100) DEFAULT NULL COMMENT '所在院系',
    title           VARCHAR(50)  DEFAULT NULL COMMENT '职称',
    phone           VARCHAR(20)  DEFAULT NULL COMMENT '联系方式',
    password        VARCHAR(100) NOT NULL COMMENT '登录密码',
    account_status  VARCHAR(10)  NOT NULL DEFAULT '启用' COMMENT '账号状态（启用/禁用）',
    PRIMARY KEY (teacher_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '教师表';

-- ---------------------------------------------------------------------
-- 3. 学生表（含所属学生组号、整体进度状态）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS t_student;
CREATE TABLE t_student (
    student_no        VARCHAR(20)  NOT NULL COMMENT '学号',
    student_name      VARCHAR(50)  NOT NULL COMMENT '姓名',
    gender            VARCHAR(4)   DEFAULT NULL COMMENT '性别（男/女）',
    class_name        VARCHAR(50)  DEFAULT NULL COMMENT '班级',
    major             VARCHAR(100) DEFAULT NULL COMMENT '专业',
    grade             VARCHAR(10)  DEFAULT NULL COMMENT '年级',
    student_group_id  INT          NOT NULL COMMENT '所属学生组号',
    password          VARCHAR(100) NOT NULL COMMENT '登录密码',
    account_status    VARCHAR(10)  NOT NULL DEFAULT '启用' COMMENT '账号状态（启用/禁用）',
    overall_status    VARCHAR(10)  NOT NULL DEFAULT '待分配' COMMENT '整体进度状态（待分配/进行中/待答辩/已完成/已弃做）',
    PRIMARY KEY (student_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '学生表';

-- ---------------------------------------------------------------------
-- 4. 学生分组表（组内学号直接存储在表中，t_student.student_group_id 仅作关联）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS t_student_group;
CREATE TABLE t_student_group (
    group_id    INT          NOT NULL AUTO_INCREMENT COMMENT '学生组号',
    group_name  VARCHAR(50)  DEFAULT NULL COMMENT '组名',
    student_no  VARCHAR(500) DEFAULT NULL COMMENT '组内学号列表（逗号分隔）',
    PRIMARY KEY (group_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '学生分组表';

-- ---------------------------------------------------------------------
-- 5. 教师分组表（固定每组 3 人：组长/秘书/普通成员）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS t_teacher_group;
CREATE TABLE t_teacher_group (
    group_id       INT          NOT NULL AUTO_INCREMENT COMMENT '教师组号',
    group_name     VARCHAR(50)  DEFAULT NULL COMMENT '组名',
    leader_no      VARCHAR(20)  NOT NULL COMMENT '组长工号',
    secretary_no   VARCHAR(20)  NOT NULL COMMENT '秘书工号',
    member_no      VARCHAR(20)  NOT NULL COMMENT '普通成员工号',
    group_status   VARCHAR(10)  NOT NULL DEFAULT '待启用' COMMENT '分组状态（待启用/已启用/已停用）',
    PRIMARY KEY (group_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '教师分组表';

-- ---------------------------------------------------------------------
-- 5. 环节对应关系表（按环节设定教师组与学生组的对应关系）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS t_group_mapping;
CREATE TABLE t_group_mapping (
    id                 INT          NOT NULL AUTO_INCREMENT COMMENT '记录编号',
    stage              VARCHAR(10)  NOT NULL COMMENT '环节（开题/中期/答辩）',
    teacher_group_id   INT          NOT NULL COMMENT '教师组号',
    student_group_id   INT          NOT NULL COMMENT '学生组号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_stage_teacher_group (stage, teacher_group_id),
    UNIQUE KEY uk_stage_student_group (stage, student_group_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '环节对应关系表';

-- ---------------------------------------------------------------------
-- 6. 师生关系表（合并指导关系与评阅关系）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS t_teacher_student;
CREATE TABLE t_teacher_student (
    id              INT          NOT NULL AUTO_INCREMENT COMMENT '记录编号',
    student_no      VARCHAR(20)  NOT NULL COMMENT '学号',
    teacher_no      VARCHAR(20)  NOT NULL COMMENT '教师工号',
    relation_type   VARCHAR(10)  NOT NULL COMMENT '关系类型（指导/评阅）',
    relation_status VARCHAR(10)  NOT NULL DEFAULT '生效' COMMENT '关系状态（生效/已解除）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_relation (student_no, relation_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '师生关系表';

-- ---------------------------------------------------------------------
-- 7. 文档表（合并任务书与指导书）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS t_document;
CREATE TABLE t_document (
    id                 INT          NOT NULL AUTO_INCREMENT COMMENT '记录编号',
    student_no         VARCHAR(20)  NOT NULL COMMENT '学号',
    doc_type           VARCHAR(10)  NOT NULL COMMENT '文档类型（任务书/指导书）',
    title              VARCHAR(200) DEFAULT NULL COMMENT '题目（任务书必填）',
    subject_category   VARCHAR(4)   DEFAULT NULL COMMENT '课题类别 A/B/C/D',
    subject_type       VARCHAR(4)   DEFAULT NULL COMMENT '课题类型 A/B/C',
    subject_new_old    VARCHAR(4)   DEFAULT NULL COMMENT '新旧课题 A/B',
    content            TEXT         DEFAULT NULL COMMENT '正文内容',
    status             VARCHAR(10)  NOT NULL DEFAULT '草稿' COMMENT '文档状态（草稿/已提交）',
    approval_status    VARCHAR(10)  DEFAULT NULL COMMENT '课题审批状态（待系审/系通过/系驳回/待院审/院通过/院驳回），仅任务书使用',
    submit_time        DATETIME     DEFAULT NULL COMMENT '提交时间',
    update_time        DATETIME     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_doc_type (student_no, doc_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '文档表';

-- ---------------------------------------------------------------------
-- 8. 评价记录表（合并各环节的成绩、评语、记录）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS t_score_record;
CREATE TABLE t_score_record (
    id            INT            NOT NULL AUTO_INCREMENT COMMENT '记录编号',
    student_no    VARCHAR(20)    NOT NULL COMMENT '学号',
    item_type     VARCHAR(20)    NOT NULL COMMENT '条目类型',
    sub_scores    VARCHAR(100)   DEFAULT NULL COMMENT '分项成绩（逗号分隔）',
    score         DECIMAL(5, 2)  DEFAULT NULL COMMENT '总成绩',
    grade         VARCHAR(10)    DEFAULT NULL COMMENT '等级（优/良/中/及格/不及格）',
    comment       VARCHAR(2000)  DEFAULT NULL COMMENT '评语/记录内容',
    recorder_no   VARCHAR(20)    NOT NULL COMMENT '录入人账号',
    record_time   DATETIME       NOT NULL COMMENT '录入时间/答辩日期',
    update_time   DATETIME       DEFAULT NULL COMMENT '更新时间',
    record_status VARCHAR(10)    NOT NULL DEFAULT '暂存' COMMENT '记录状态（暂存/已确认）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_item (student_no, item_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '评价记录表';

-- ---------------------------------------------------------------------
-- 9. 环节状态表（跟踪每名学生在三个环节的进度）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS t_stage_status;
CREATE TABLE t_stage_status (
    id             INT          NOT NULL AUTO_INCREMENT COMMENT '记录编号',
    student_no     VARCHAR(20)  NOT NULL COMMENT '学号',
    stage          VARCHAR(10)  NOT NULL COMMENT '环节（开题/中期/答辩）',
    status         VARCHAR(10)  NOT NULL DEFAULT '未开始' COMMENT '状态（未开始/进行中/已完成）',
    start_time     DATETIME     DEFAULT NULL COMMENT '开始时间',
    complete_time  DATETIME     DEFAULT NULL COMMENT '完成时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_stage (student_no, stage)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '环节状态表';

-- ---------------------------------------------------------------------
-- 10. 操作日志表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS t_operation_log;
CREATE TABLE t_operation_log (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录编号',
    user_type       VARCHAR(10)  NOT NULL COMMENT '用户类型（admin/teacher/student）',
    user_no         VARCHAR(20)  NOT NULL COMMENT '用户账号',
    operation       VARCHAR(500) DEFAULT NULL COMMENT '操作内容',
    operation_time  DATETIME     NOT NULL COMMENT '操作时间',
    ip              VARCHAR(50)  DEFAULT NULL COMMENT 'IP 地址',
    PRIMARY KEY (id),
    KEY idx_user (user_type, user_no),
    KEY idx_operation_time (operation_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '操作日志表';

-- =====================================================================
-- 初始化数据：插入一个默认超级管理员账号（密码：admin123）
-- =====================================================================
INSERT INTO t_admin (admin_id, admin_name, password) VALUES
    ('admin', '系统管理员', 'admin123');
