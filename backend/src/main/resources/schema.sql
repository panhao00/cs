-- CRS 数据库 Schema
-- 兼容 H2(MySQL 模式) 与 MySQL 8.x
-- 参考 AI 生成,经人工核验与调整

-- 用户表(单表存储三类角色: STUDENT / PROFESSOR / REGISTRAR)
CREATE TABLE IF NOT EXISTS app_user (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role         VARCHAR(20)  NOT NULL,           -- STUDENT, PROFESSOR, REGISTRAR
    name         VARCHAR(100) NOT NULL,
    dob          DATE,
    ssn          VARCHAR(20),
    status       VARCHAR(20)  DEFAULT 'ACTIVE',  -- ACTIVE, INACTIVE
    graduation_date DATE,                         -- 仅学生
    department   VARCHAR(50),                     -- 仅教授
    created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- 课程目录(只读,模拟遗留 Ingres Catalog)
CREATE TABLE IF NOT EXISTS course (
    course_id        VARCHAR(20)  PRIMARY KEY,
    title            VARCHAR(200) NOT NULL,
    department       VARCHAR(50),
    credits          INT          DEFAULT 3,
    prerequisite_id  VARCHAR(20),
    FOREIGN KEY (prerequisite_id) REFERENCES course(course_id)
);

-- 课程开设(某学期的具体班级)
CREATE TABLE IF NOT EXISTS course_offering (
    offering_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id     VARCHAR(20)  NOT NULL,
    semester      VARCHAR(20)  NOT NULL,        -- 如 2026-FALL
    professor_id  BIGINT,                       -- 教授 ID(可为空,表示尚未指派)
    days          VARCHAR(20),                   -- 如 MWF / TR
    times         VARCHAR(50),                   -- 如 09:00-10:00
    room          VARCHAR(50),
    max_capacity  INT          DEFAULT 10,
    status        VARCHAR(20)  DEFAULT 'OPEN',  -- OPEN, COMMITTED, CLOSED, CANCELLED
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    version       BIGINT       DEFAULT 0,
    FOREIGN KEY (course_id) REFERENCES course(course_id),
    FOREIGN KEY (professor_id) REFERENCES app_user(id)
);

-- 课表(学生某学期的选课计划)
CREATE TABLE IF NOT EXISTS schedule (
    schedule_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id   BIGINT       NOT NULL,
    semester     VARCHAR(20)  NOT NULL,
    state        VARCHAR(20)  DEFAULT 'DRAFT',  -- DRAFT, COMMITTED, CLOSED
    created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES app_user(id)
);

-- 课表项(课表中的每一门课)
CREATE TABLE IF NOT EXISTS schedule_item (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    schedule_id  BIGINT      NOT NULL,
    offering_id  BIGINT      NOT NULL,
    kind         VARCHAR(20) NOT NULL,           -- PRIMARY, ALTERNATE
    status       VARCHAR(20) DEFAULT 'SELECTED', -- SELECTED, ENROLLED_IN, CANCELLED
    FOREIGN KEY (schedule_id) REFERENCES schedule(schedule_id),
    FOREIGN KEY (offering_id) REFERENCES course_offering(offering_id)
);

-- 选课记录(学生被正式加入某开设的名单)
CREATE TABLE IF NOT EXISTS enrollment (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    offering_id BIGINT      NOT NULL,
    student_id  BIGINT      NOT NULL,
    status      VARCHAR(20) DEFAULT 'ENROLLED',  -- ENROLLED, CANCELLED
    enrolled_at TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (offering_id) REFERENCES course_offering(offering_id),
    FOREIGN KEY (student_id) REFERENCES app_user(id),
    UNIQUE(offering_id, student_id)
);

-- 成绩
CREATE TABLE IF NOT EXISTS grade (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    offering_id BIGINT      NOT NULL,
    student_id  BIGINT      NOT NULL,
    grade       VARCHAR(2),                       -- A, B, C, D, F, I, 或 NULL
    updated_at  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (offering_id) REFERENCES course_offering(offering_id),
    FOREIGN KEY (student_id) REFERENCES app_user(id),
    UNIQUE(offering_id, student_id)
);

-- 账单(关闭注册后生成)
CREATE TABLE IF NOT EXISTS bill (
    bill_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id  BIGINT        NOT NULL,
    semester    VARCHAR(20)   NOT NULL,
    amount      DECIMAL(10,2) NOT NULL,
    status      VARCHAR(20)   DEFAULT 'PENDING',  -- PENDING, SENT, FAILED
    sent_at     TIMESTAMP,
    created_at  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES app_user(id)
);

-- 审计日志(记录敏感操作: 登录、成绩查看等)
CREATE TABLE IF NOT EXISTS audit_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_id    BIGINT,
    action      VARCHAR(100) NOT NULL,
    target_type VARCHAR(50),
    target_id   BIGINT,
    details     VARCHAR(1000),
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- 通知(学生通知: 课程取消、选课成功/失败、注册关闭、成绩录入等)
CREATE TABLE IF NOT EXISTS notification (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id  BIGINT       NOT NULL,
    type        VARCHAR(30)  NOT NULL,           -- OFFERING_CANCELLED, ENROLLMENT_SUCCESS, ENROLLMENT_FAILED, REGISTRATION_CLOSED, GRADE_POSTED, SYSTEM
    title       VARCHAR(200) NOT NULL,
    content     TEXT,
    offering_id BIGINT,
    course_id   VARCHAR(20),
    is_read     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES app_user(id)
);

-- 系统设置(键值对存储,用于持久化注册周期状态等)
CREATE TABLE IF NOT EXISTS system_setting (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key   VARCHAR(100) NOT NULL UNIQUE,
    setting_value VARCHAR(500) NOT NULL,
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- ==================== 性能索引 ====================
-- Note: H2 supports CREATE INDEX IF NOT EXISTS; MySQL does not.
-- For MySQL production, index creation errors are expected on restart (indexes already exist).
-- Use spring.sql.init.continue-on-error=true in prod profile.
CREATE INDEX IF NOT EXISTS idx_offering_semester_status ON course_offering(semester, status);
CREATE INDEX IF NOT EXISTS idx_offering_professor ON course_offering(professor_id);
CREATE INDEX IF NOT EXISTS idx_schedule_student_semester ON schedule(student_id, semester);
CREATE INDEX IF NOT EXISTS idx_schedule_item_schedule ON schedule_item(schedule_id);
CREATE INDEX IF NOT EXISTS idx_enrollment_offering ON enrollment(offering_id, status);
CREATE INDEX IF NOT EXISTS idx_enrollment_student ON enrollment(student_id, status);
CREATE INDEX IF NOT EXISTS idx_grade_student ON grade(student_id);
CREATE INDEX IF NOT EXISTS idx_bill_student ON bill(student_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_actor ON audit_log(actor_id);
CREATE INDEX IF NOT EXISTS idx_notification_student ON notification(student_id, is_read);
