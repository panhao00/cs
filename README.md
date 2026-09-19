# 课程注册系统 (Course Registration System / CRS)

> Wylie College 课程注册系统 —— 替代遗留大型机系统,采用 B/S 架构。

## 技术栈

| 层     | 技术                                                        |
|--------|-------------------------------------------------------------|
| 前端   | Vue 3 + Vite 8 + Element Plus + Pinia + Vue Router          |
| 后端   | Spring Boot 3.3 + Java 21 + Spring Security + JPA/Hibernate |
| 数据库 | 开发: H2 内存库 / 生产: MySQL 8.x                           |
| 认证   | JWT (jjwt 0.12) + BCrypt 密码哈希                           |

## 项目结构

```
E:\cs\
├── backend/                              # Spring Boot 后端
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd                  # Maven Wrapper(无需全局安装 Maven)
│   ├── src/main/java/com/crs/
│   │   ├── CrsApplication.java           # 启动入口
│   │   ├── DataInitializer.java          # 种子数据(默认账号+课程)
│   │   ├── domain/                       # 领域模型(13 实体 + 8 枚举)
│   │   │   ├── User.java  Course.java  CourseOffering.java
│   │   │   ├── Schedule.java  ScheduleItem.java  Enrollment.java
│   │   │   ├── Grade.java  Bill.java  AuditLog.java  Notification.java
│   │   │   └── Role.java  OfferingStatus.java  ScheduleState.java  ...
│   │   ├── repository/                   # JPA Repository(13 个)
│   │   ├── service/                      # 业务逻辑(31 个 Service)
│   │   │   ├── AuthService.java          # 登录认证
│   │   │   ├── UserService.java          # 用户管理
│   │   │   ├── ScheduleService.java      # 课表管理(选课/退课)
│   │   │   ├── TeachingService.java      # 教授选课授课
│   │   │   ├── GradeService.java         # 成绩录入
│   │   │   ├── ReportCardService.java    # 成绩单查询
│   │   │   ├── RegistrationService.java  # 关闭注册
│   │   │   ├── CatalogService.java       # 课程目录
│   │   │   ├── NotificationService.java  # 通知管理
│   │   │   ├── AuditService.java         # 审计日志
│   │   │   ├── BillingAsyncService.java  # 异步计费通知(@Async)
│   │   │   ├── BillingRetryService.java  # 计费失败重试
│   │   │   ├── EnrollmentStatsService.java # 选课统计
│   │   │   ├── GradeStatsService.java    # 成绩统计
│   │   │   ├── SystemSettingService.java # 系统设置
│   │   │   ├── ReviewService.java        # 课程评价
│   │   │   ├── WaitlistService.java      # 候补名单
│   │   │   ├── RecommendationService.java # 课程推荐
│   │   │   ├── CapacityNotificationService.java # 容量预警通知
│   │   │   ├── DeadlineReminderService.java # 截止日期提醒
│   │   │   └── AuditLogQueryService.java # 审计日志查询
│   │   ├── security/                     # 安全配置(9 个)
│   │   │   ├── SecurityConfig.java       # Spring Security + JWT + CORS
│   │   │   ├── JwtTokenProvider.java     # JWT 生成/校验
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   ├── CrsUserDetails.java       # UserDetails 适配器
│   │   │   ├── CrsUserDetailsService.java
│   │   │   ├── GradeEncryptionConverter.java # 成绩加密转换器(环境变量密钥)
│   │   │   ├── EncryptionUtil.java       # 通用加密工具
│   │   │   ├── DataMaskUtil.java         # 数据脱敏(SSN等)
│   │   │   └── SecurityContextHelper.java # 安全上下文辅助
│   │   ├── web/                          # REST Controller(10 个)
│   │   │   ├── AuthController.java       # POST /api/auth/login
│   │   │   ├── HealthController.java     # GET /api/health
│   │   │   ├── CatalogController.java    # 课程目录查询
│   │   │   ├── ScheduleController.java   # 课表 CRUD
│   │   │   ├── TeachingController.java   # 教授选课
│   │   │   ├── GradeController.java      # 成绩录入
│   │   │   ├── ReportCardController.java # 成绩单
│   │   │   ├── UserController.java       # 用户 CRUD
│   │   │   ├── RegistrationController.java # 关闭注册
│   │   │   ├── NotificationController.java # 通知
│   │   │   └── dto/                      # 请求/响应 DTO
│   │   └── common/                       # 通用组件(7 个)
│   │       ├── ApiException.java         # 业务异常
│   │       ├── CatalogUnavailableException.java
│   │       ├── GlobalExceptionHandler.java
│   │       ├── RegistrationPhase.java    # 注册阶段状态机
│   │       ├── PrerequisiteChecker.java  # 先修课检查
│   │       ├── SemesterConfig.java       # 学期配置
│   │       └── TimeConflictUtil.java     # 时间冲突检测工具
│   ├── src/main/resources/
│   │   ├── application.yml               # 主配置
│   │   ├── application-dev.yml           # H2 开发环境
│   │   ├── application-prod.yml          # MySQL 生产环境
│   │   └── schema.sql                    # 数据库 DDL
│   └── src/test/java/com/crs/            # 单元测试(113 用例)
│       └── service/
│           ├── ScheduleServiceTest.java
│           ├── GradeServiceTest.java
│           ├── TeachingServiceTest.java
│           ├── UserServiceTest.java
│           ├── ReportCardServiceTest.java
│           ├── AuthServiceTest.java
│           └── RegistrationServiceTest.java
├── frontend/                             # Vue 3 前端
│   ├── package.json
│   ├── vite.config.js                    # Vite 配置
│   ├── vitest.config.js                  # Vitest 测试配置
│   ├── index.html
│   ├── login-test.html                   # 免 Node.js 静态测试页面
│   └── src/
│       ├── main.js
│       ├── App.vue
│       ├── router/index.js               # 路由守卫(认证 + 角色检查)
│       ├── stores/
│       │   └── auth.js                   # Pinia 认证状态
│       ├── api/                          # API 客户端
│       │   ├── index.js                  # axios 实例(JWT 拦截器)
│       │   ├── courses.js                # 课程目录 + 课表 API
│       │   ├── users.js                  # 用户 + 注册管理 API
│       │   ├── teaching.js               # 教学 + 成绩 + 成绩单 API
│       │   └── notifications.js          # 通知 API
│       ├── components/
│       │   └── AppLayout.vue             # 共用页面布局组件
│       ├── views/                        # 18 个页面视图
│       │   ├── Login.vue                 # 登录页
│       │   ├── Register.vue              # 注册页
│       │   ├── Dashboard.vue             # 仪表盘(角色快捷操作)
│       │   ├── RegisterCourses.vue       # 学生选课
│       │   ├── TeachCourses.vue          # 教授选课授课
│       │   ├── SubmitGrades.vue          # 提交成绩
│       │   ├── ReportCard.vue            # 查看成绩单
│       │   ├── UserManagement.vue        # 用户维护(注册主任)
│       │   ├── RegistrationManagement.vue # 关闭注册(注册主任)
│       │   ├── Notifications.vue         # 通知中心
│       │   ├── CourseRecommendation.vue  # 课程推荐
│       │   ├── CourseReviews.vue         # 课程评价
│       │   ├── Waitlist.vue              # 候补名单
│       │   ├── EnrollmentDashboard.vue   # 选课统计仪表盘
│       │   ├── GradeTrend.vue            # 成绩趋势分析
│       │   ├── SystemSettings.vue        # 系统设置
│       │   ├── AuditLogs.vue             # 审计日志查看
│       │   └── ChangePassword.vue        # 修改密码
│       └── __tests__/                    # 前端单元测试(46 用例)
│           ├── auth.test.js              # 认证 Store 测试
│           ├── api.test.js               # API 拦截器测试
│           └── guards.test.js            # 路由守卫测试
├── docs/                                 # 项目文档
│   ├── 设计文档.md                        # 系统设计(ER 图/类图/时序图)
│   ├── 开发计划.md                        # 迭代计划与风险管理
│   ├── 测试计划.md                        # 测试策略与用例
│   ├── 测试报告.md                        # 测试执行结果
│   └── diagrams/                         # PlantUML 架构图
│       ├── use-case-diagram.puml         # 用例图
│       ├── class-diagram.puml            # 类图
│       ├── er-diagram.puml               # ER 图
│       ├── component-diagram.puml        # 组件图
│       └── sequence-diagrams.puml        # 时序图
├── tools/                                # 本地 Maven(不入版本库)
├── start-docker.ps1                     # Windows Docker 一键启动脚本
├── start-docker.sh                      # Linux/macOS Docker 一键启动脚本
├── docker-compose.yml                   # Docker Compose 配置
├── .env.example                         # 环境变量模板
└── 课程注册系统_开发指南.md               # 需求规格说明
```

## 快速开始

### 容器化部署（推荐）

**前置条件:** Docker Desktop 已安装并运行

```powershell
# Windows (PowerShell)
.\start-docker.ps1

# Linux/macOS
chmod +x start-docker.sh
./start-docker.sh
```

脚本会自动：
- 检查 Docker 环境
- 生成必要的安全密钥（JWT_SECRET, GRADE_ENCRYPTION_KEY）
- 构建并启动所有服务（MySQL + 3个后端实例 + Nginx负载均衡 + 前端）

启动后访问：
- **前端:** http://localhost
- **后端API:** http://localhost:8443/api
- **健康检查:** http://localhost/health

常用命令：
```powershell
# 查看日志
.\start-docker.ps1 -Logs

# 停止服务
.\start-docker.ps1 -Stop

# 查看服务状态
docker compose ps

# 重启服务
docker compose restart
```

### 后端(无需安装 Maven)

**前置条件:** JDK 21+ (已安装)

```powershell
cd E:\cs\backend

# 方式一: 使用 Maven Wrapper(首次运行自动下载 Maven)
.\mvnw.cmd spring-boot:run

# 方式二: 使用本地 Maven
$env:Path = "E:\cs\tools\apache-maven-3.9.9\bin;$env:Path"
mvn spring-boot:run
```

后端启动后:
- API 地址: http://localhost:8080/api
- 健康检查: http://localhost:8080/api/health
- H2 控制台: http://localhost:8080/api/h2-console (JDBC URL: `jdbc:h2:mem:crs`, 用户: `sa`)

### 前端

**前置条件:** Node.js 18+ (需安装)

```powershell
cd E:\cs\frontend
npm install
npm run dev
```

前端启动后: http://localhost:5173

> **免 Node.js 快速测试:** 直接在浏览器打开 `frontend/login-test.html`

### 生产环境(MySQL)

**前置条件:** MySQL 8.x (需单独安装并启动)

```powershell
# 启动 MySQL (生产环境必需,MySQL 以独立进程运行)
Start-Process "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe" -ArgumentList "--basedir=`"C:\Program Files\MySQL\MySQL Server 8.4`"", "--datadir=`"C:\ProgramData\MySQL\MySQL Server 8.4\Data`"", "--port=3306"

# 启动后端(prod profile)
cd E:\cs\backend
$env:DB_URL = "jdbc:mysql://localhost:3306/crs?useSSL=false&serverTimezone=Asia/Shanghai"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "crs123456"
$env:JWT_SECRET = "your-strong-secret-key"
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=prod
```

## 环境变量

### 容器化部署（使用 docker-compose）

| 变量                   | 必填 | 说明                                            |
|------------------------|------|-------------------------------------------------|
| `JWT_SECRET`           | 是   | JWT 签名密钥(≥32 字符),启动脚本会自动生成       |
| `GRADE_ENCRYPTION_KEY` | 是   | 成绩加密密钥(32 字节 Base64),启动脚本会自动生成 |
| `MYSQL_ROOT_PASSWORD`  | 否   | MySQL root 密码(默认: crs123456)                |
| `MYSQL_PASSWORD`       | 否   | MySQL 应用用户密码(默认: crs123456)             |

### 手动部署

| 变量                   | 必填     | 说明                                        |
|------------------------|----------|---------------------------------------------|
| `JWT_SECRET`           | 生产环境 | JWT 签名密钥(≥32 字符)                      |
| `GRADE_ENCRYPTION_KEY` | 可选     | 成绩加密密钥(32 字节 Base64),不设则自动生成 |
| `DB_URL`               | 生产环境 | MySQL JDBC URL                              |
| `DB_USERNAME`          | 生产环境 | 数据库用户名                                |
| `DB_PASSWORD`          | 生产环境 | 数据库密码                                  |

## 默认测试账号

| 用户名                 | 密码                   | 角色     | 姓名              | 部门             |
|------------------------|------------------------|----------|-------------------|------------------|
| `registrar`            | `registrar`            | 注册主任 | Alice Registrar   | -                |
| `professor`            | `professor`            | 教授     | Bob Smith         | Computer Science |
| `professor2`           | `professor2`           | 教授     | Diana Johnson     | Mathematics      |
| `professor3`           | `professor3`           | 教授     | Edward Williams   | English          |
| `professor4`           | `professor4`           | 教授     | Fiona Davis       | Physics          |
| `professor5`           | `professor5`           | 教授     | George Miller     | Computer Science |
| `student`              | `student`              | 学生     | Charlie Brown     | Computer Science |
| `student2`~`student30` | `student2`~`student30` | 学生     | (30名学生,多院系) | CS/Math/Phys/Eng |

### 种子数据概览

- **15 门课程**: CS (7门) + Math (4门) + English (2门) + Physics (2门)
- **22 个课程开设**: 17 本学期 (2026-FALL) + 5 上学期 (2026-SPRING)
- **36 个用户**: 1 Registrar + 5 Professors + 30 Students
- **课程先修链**: CS101→CS102→CS201→CS301, MATH101→MATH102→MATH201→MATH301, PHYS101→PHYS201

## API 端点

### 认证与系统

| 方法 | 路径              | 认证 | 说明          |
|------|-------------------|------|---------------|
| POST | `/api/auth/login` | 公开 | 登录,返回 JWT |
| GET  | `/api/health`     | 公开 | 健康检查      |

### 课程目录

| 方法 | 路径                          | 认证 | 说明                 |
|------|-------------------------------|------|----------------------|
| GET  | `/api/catalog/offerings`      | 需要 | 查询课程开设列表     |
| GET  | `/api/catalog/offerings/{id}` | 需要 | 查询单个课程开设详情 |

### 课表管理(学生)

| 方法   | 路径                                     | 认证    | 说明           |
|--------|------------------------------------------|---------|----------------|
| GET    | `/api/schedules`                         | STUDENT | 获取我的课表   |
| POST   | `/api/schedules`                         | STUDENT | 创建课表       |
| POST   | `/api/schedules/{id}/items`              | STUDENT | 添加课程到课表 |
| DELETE | `/api/schedules/{id}/items/{offeringId}` | STUDENT | 从课表移除课程 |
| POST   | `/api/schedules/{id}/submit`             | STUDENT | 提交课表(选课) |
| DELETE | `/api/schedules/{id}`                    | STUDENT | 删除课表       |

### 教授选课

| 方法 | 路径                      | 认证      | 说明           |
|------|---------------------------|-----------|----------------|
| GET  | `/api/teaching/offerings` | PROFESSOR | 查询可授课开设 |
| POST | `/api/teaching/select`    | PROFESSOR | 提交授课选择   |

### 成绩管理

| 方法 | 路径                              | 认证      | 说明               |
|------|-----------------------------------|-----------|--------------------|
| GET  | `/api/grades/taught`              | PROFESSOR | 获取上学期授课列表 |
| GET  | `/api/grades/roster/{offeringId}` | PROFESSOR | 获取班级名单       |
| POST | `/api/grades/submit`              | PROFESSOR | 录入/修改成绩      |

### 成绩单

| 方法 | 路径               | 认证    | 说明           |
|------|--------------------|---------|----------------|
| GET  | `/api/report-card` | STUDENT | 查看我的成绩单 |

### 用户管理(注册主任)

| 方法   | 路径                     | 认证      | 说明               |
|--------|--------------------------|-----------|--------------------|
| GET    | `/api/users`             | REGISTRAR | 获取用户列表       |
| POST   | `/api/users`             | REGISTRAR | 创建用户           |
| PUT    | `/api/users/{id}`        | REGISTRAR | 更新用户信息       |
| PATCH  | `/api/users/{id}/status` | REGISTRAR | 激活/停用用户      |
| DELETE | `/api/users/{id}`        | REGISTRAR | 删除用户(仅非活跃) |

### 注册管理(注册主任)

| 方法 | 路径                        | 认证      | 说明             |
|------|-----------------------------|-----------|------------------|
| GET  | `/api/registration/status`  | REGISTRAR | 获取注册阶段状态 |
| GET  | `/api/registration/preview` | REGISTRAR | 预览关闭注册结果 |
| POST | `/api/registration/close`   | REGISTRAR | 执行关闭注册     |

### 通知

| 方法 | 路径                           | 认证 | 说明         |
|------|--------------------------------|------|--------------|
| GET  | `/api/notifications`           | 需要 | 获取通知列表 |
| POST | `/api/notifications/{id}/read` | 需要 | 标记单条已读 |
| POST | `/api/notifications/read-all`  | 需要 | 全部标为已读 |

### Login 请求示例

```bash
POST /api/auth/login
Content-Type: application/json

{"username": "student", "password": "student"}
```

### Login 响应示例

```json
{
  "token": "eyJhbGci...",
  "tokenType": "Bearer",
  "userId": 3,
  "username": "student",
  "name": "Charlie Brown",
  "role": "STUDENT",
  "expiresAt": 1789625873430
}
```

后续请求携带 JWT:
```
Authorization: Bearer eyJhbGci...
```

## 已实现 / 待实现

| 用例                                 | 后端 | 前端 | 说明                                              |
|--------------------------------------|------|------|---------------------------------------------------|
| 7.1 Login 登录                       | ✅   | ✅   | JWT 认证 + BCrypt                                 |
| 7.2 Register for Courses 学生选课    | ✅   | ✅   | 课表管理 + 时间冲突检测 + 提交/保存               |
| 7.3 Select Courses to Teach 教授选课 | ✅   | ✅   | 教授选择教授开设                                  |
| 7.4 Submit Grades 提交成绩           | ✅   | ✅   | 成绩录入 + 通知学生                               |
| 7.5 View Report Card 查看成绩单      | ✅   | ✅   | 学生成绩查询                                      |
| 7.6 Maintain Professor Information   | ✅   | ✅   | 注册主任管理教授                                  |
| 7.7 Maintain Student Information     | ✅   | ✅   | 注册主任管理学生                                  |
| 7.8 Close Registration 关闭注册      | ✅   | ✅   | 完整算法: commit/cancel + leveling + Billing 通知 |

### 已完成的基础设施

- ✅ 三层 B/S 架构 (Vue → Spring Boot REST → JPA → DB)
- ✅ 领域模型 (10 实体 + 8 枚举,覆盖第 9.3 节全部表)
- ✅ JPA Repository (10 个,含按学期/状态/学生等查询方法)
- ✅ 业务逻辑 (21 个 Service,覆盖全部 8 个用例 + 异步计费/重试/统计/评价/候补/推荐)
- ✅ REST Controller (10 个,35+ 端点)
- ✅ JWT 认证 + BCrypt 密码哈希 + Spring Security 无状态配置
- ✅ 成绩加密转换 (GradeEncryptionConverter, 环境变量 `GRADE_ENCRYPTION_KEY`)
- ✅ 数据脱敏 (DataMaskUtil, SSN 等敏感信息遮罩)
- ✅ CORS 配置 (放行前端开发服务器)
- ✅ `@EnableAsync` 异步支持 (BillingAsyncService 异步计费通知)
- ✅ 集中式审计日志 (AuditService, 记录登录/选课/成绩等操作)
- ✅ 全局异常处理 (统一 ErrorResponse 格式)
- ✅ 注册阶段状态机 (RegistrationPhase: open → add/drop → closed)
- ✅ 时间冲突检测工具 (TimeConflictUtil, ScheduleService/TeachingService 共用)
- ✅ 先修课检查 (PrerequisiteChecker)
- ✅ H2 开发 / MySQL 生产 双 profile
- ✅ 种子数据 (36 用户 + 15 课程 + 22 开设)
- ✅ Vue 3 前端 (18 个视图 + 1 个共用组件 + 路由守卫 + JWT 拦截器 + Element Plus)
- ✅ 前端路由守卫 (认证检查 + 角色 RBAC: STUDENT/PROFESSOR/REGISTRAR)
- ✅ Vite 8 构建 (代码拆分: element-plus / vendor / main)
- ✅ npm audit 0 vulnerabilities
- ✅ 通知系统 (Notification 实体 + Service + Controller + 前端通知中心)
- ✅ 后端单元测试 (113 用例,覆盖全部 Service)
- ✅ 前端单元测试 (46 用例,覆盖 Auth Store/API 拦截器/路由守卫)
- ✅ Docker 容器化部署 (多阶段构建,一键启动脚本)

## 已知限制

1. **容器化部署需要 Docker** — 推荐使用 Docker Desktop，或手动安装 Docker Engine。
2. **课程目录为模拟实现** — 使用本地 H2/MySQL 表模拟遗留 Ingres Catalog,后续需实现 Adapter + 缓存层(需求第 9.2 节)。
3. **开发环境可选** — 使用 `start-docker.ps1` 或 `start-docker.sh` 可一键启动完整环境，无需单独安装 Node.js、JDK 或 MySQL。

## UML 架构图

`docs/diagrams/` 目录包含 5 个 PlantUML 源文件，可使用 PlantUML 插件或在线渲染器生成图片：

| 文件                     | 内容                                |
|--------------------------|-------------------------------------|
| `use-case-diagram.puml`  | 系统用例图 (7.x 全部用例)           |
| `class-diagram.puml`     | 领域模型类图 (10 实体 + 8 枚举)     |
| `er-diagram.puml`        | 数据库 ER 图                        |
| `component-diagram.puml` | 系统组件架构图                      |
| `sequence-diagrams.puml` | 核心业务时序图 (选课/成绩/关闭注册) |

渲染方式：安装 VS Code PlantUML 插件后在 `.puml` 文件中按 `Alt+D` 预览，或访问 [PlantUML Online Server](https://www.plantuml.com/plantuml/uml/)。

## AI 使用说明(符合第 11 节规范)

本项目代码由 AI 编程助手辅助生成,遵循以下原则:

- ✅ AI 用于: 项目骨架搭建、代码片段参考、配置文件生成
- ✅ 核心设计决策(单表用户模型、JWT 认证、H2/MySQL 双 profile、审计日志)由人工决定
- ✅ 代码中标注 "参考 AI 生成,经人工核验与调整"
- ⚠️ 后续迭代中,核心业务逻辑(选课校验、Close Registration 算法、并发控制)须由小组成员自主设计,AI 仅作参考

**修改点记录:**
1. User 实体采用单表策略而非 JOINED 继承(简化查询,字段重叠度高)
2. SecurityConfig 手动配置 CORS 放行端口(非 AI 默认的通配)
3. DataInitializer 使用 BCrypt 编程式哈希(非 SQL 硬编码哈希)
4. 审计日志在 AuthService 中手动记录(非 AOP 自动化,便于控制粒度)
