# 课程注册系统 (Course Registration System / CRS)

> Wylie College 课程注册系统 —— 替代遗留大型机系统,采用 B/S 架构。

## 技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vue 3 + Vite + Element Plus + Pinia + Vue Router |
| 后端 | Spring Boot 3.3 + Java 21 + Spring Security + JPA/Hibernate |
| 数据库 | 开发: H2 内存库 / 生产: MySQL 8.x |
| 认证 | JWT (jjwt 0.12) + BCrypt 密码哈希 |

## 项目结构

```
E:\cs\
├── backend/                              # Spring Boot 后端
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd                  # Maven Wrapper(无需全局安装 Maven)
│   ├── src/main/java/com/crs/
│   │   ├── CrsApplication.java           # 启动入口
│   │   ├── DataInitializer.java          # 种子数据(默认账号+课程)
│   │   ├── domain/                       # 领域模型(10 实体 + 8 枚举)
│   │   │   ├── User.java  Course.java  CourseOffering.java
│   │   │   ├── Schedule.java  ScheduleItem.java  Enrollment.java
│   │   │   ├── Grade.java  Bill.java  AuditLog.java  Notification.java
│   │   │   └── Role.java  OfferingStatus.java  ScheduleState.java  ...
│   │   ├── repository/                   # JPA Repository(10 个)
│   │   ├── service/                      # 业务逻辑(10 个 Service)
│   │   │   ├── AuthService.java          # 登录认证
│   │   │   ├── UserService.java          # 用户管理
│   │   │   ├── ScheduleService.java      # 课表管理(选课/退课)
│   │   │   ├── TeachingService.java      # 教授选课授课
│   │   │   ├── GradeService.java         # 成绩录入
│   │   │   ├── ReportCardService.java    # 成绩单查询
│   │   │   ├── RegistrationService.java  # 关闭注册
│   │   │   ├── CatalogService.java       # 课程目录
│   │   │   ├── NotificationService.java  # 通知管理
│   │   │   └── AuditService.java         # 审计日志
│   │   ├── security/                     # 安全配置
│   │   │   ├── SecurityConfig.java       # Spring Security + JWT + CORS
│   │   │   ├── JwtTokenProvider.java     # JWT 生成/校验
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   ├── CrsUserDetails.java       # UserDetails 适配器
│   │   │   └── CrsUserDetailsService.java
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
│   │   └── common/                       # 通用组件
│   │       ├── ApiException.java         # 业务异常
│   │       ├── GlobalExceptionHandler.java
│   │       ├── RegistrationPhase.java    # 注册阶段状态机
│   │       └── TimeConflictUtil.java     # 时间冲突检测工具
│   ├── src/main/resources/
│   │   ├── application.yml               # 主配置
│   │   ├── application-dev.yml           # H2 开发环境
│   │   ├── application-prod.yml          # MySQL 生产环境
│   │   └── schema.sql                    # 数据库 DDL
│   └── src/test/java/com/crs/            # 单元测试(80+ 用例)
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
│       ├── views/                        # 9 个页面视图
│       │   ├── Login.vue                 # 登录页
│       │   ├── Dashboard.vue             # 仪表盘(角色快捷操作)
│       │   ├── RegisterCourses.vue       # 学生选课
│       │   ├── TeachCourses.vue          # 教授选课授课
│       │   ├── SubmitGrades.vue          # 提交成绩
│       │   ├── ReportCard.vue            # 查看成绩单
│       │   ├── UserManagement.vue        # 用户维护(注册主任)
│       │   ├── RegistrationManagement.vue # 关闭注册(注册主任)
│       │   └── Notifications.vue         # 通知中心
│       └── __tests__/                    # 前端单元测试(46 用例)
│           ├── auth.test.js              # 认证 Store 测试
│           ├── api.test.js               # API 拦截器测试
│           └── guards.test.js            # 路由守卫测试
├── docs/                                 # 项目文档
│   ├── 设计文档.md                        # 系统设计(ER 图/类图/时序图)
│   ├── 开发计划.md                        # 迭代计划与风险管理
│   ├── 测试计划.md                        # 测试策略与用例
│   └── 测试报告.md                        # 测试执行结果
├── tools/                                # 本地 Maven(不入版本库)
└── 课程注册系统_开发指南.md               # 需求规格说明
```

## 快速开始

### 后端(无需安装 Maven)

**前置条件:** JDK 21+ (已安装)

```powershell
cd f:\cs\backend

# 方式一: 使用 Maven Wrapper(首次运行自动下载 Maven)
.\mvnw.cmd spring-boot:run

# 方式二: 使用本地 Maven
$env:Path = "f:\cs\tools\apache-maven-3.9.9\bin;$env:Path"
mvn spring-boot:run
```

后端启动后:
- API 地址: http://localhost:8080/api
- 健康检查: http://localhost:8080/api/health
- H2 控制台: http://localhost:8080/api/h2-console (JDBC URL: `jdbc:h2:mem:crs`, 用户: `sa`)

### 前端

**前置条件:** Node.js 18+ (需安装)

```powershell
cd f:\cs\frontend
npm install
npm run dev
```

前端启动后: http://localhost:5173

> **免 Node.js 快速测试:** 直接在浏览器打开 `frontend/login-test.html`

### 生产环境(MySQL)

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/crs?useSSL=false&serverTimezone=Asia/Shanghai"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your_password"
$env:JWT_SECRET = "your-strong-secret-key"
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=prod
```

## 默认测试账号

| 用户名 | 密码 | 角色 | 姓名 |
|---|---|---|---|
| `registrar` | `registrar` | 注册主任 | Alice Registrar |
| `professor` | `professor` | 教授 | Bob Smith (Computer Science) |
| `student` | `student` | 学生 | Charlie Brown |
| `student2`~`student12` | `student2`~`student12` | 学生 | (用于容量测试) |

种子数据还包含 5 门课程 (CS101, CS201, CS301, MATH101, ENG101) 和 4 个课程开设 (2026-FALL 学期)。

## API 端点

### 认证与系统

| 方法 | 路径 | 认证 | 说明 |
|---|---|---|---|
| POST | `/api/auth/login` | 公开 | 登录,返回 JWT |
| GET | `/api/health` | 公开 | 健康检查 |

### 课程目录

| 方法 | 路径 | 认证 | 说明 |
|---|---|---|---|
| GET | `/api/catalog/offerings` | 需要 | 查询课程开设列表 |
| GET | `/api/catalog/offerings/{id}` | 需要 | 查询单个课程开设详情 |

### 课表管理(学生)

| 方法 | 路径 | 认证 | 说明 |
|---|---|---|---|
| GET | `/api/schedules` | STUDENT | 获取我的课表 |
| POST | `/api/schedules` | STUDENT | 创建课表 |
| POST | `/api/schedules/{id}/items` | STUDENT | 添加课程到课表 |
| DELETE | `/api/schedules/{id}/items/{offeringId}` | STUDENT | 从课表移除课程 |
| POST | `/api/schedules/{id}/submit` | STUDENT | 提交课表(选课) |
| DELETE | `/api/schedules/{id}` | STUDENT | 删除课表 |

### 教授选课

| 方法 | 路径 | 认证 | 说明 |
|---|---|---|---|
| GET | `/api/teaching/offerings` | PROFESSOR | 查询可授课开设 |
| POST | `/api/teaching/select` | PROFESSOR | 提交授课选择 |

### 成绩管理

| 方法 | 路径 | 认证 | 说明 |
|---|---|---|---|
| GET | `/api/grades/taught` | PROFESSOR | 获取上学期授课列表 |
| GET | `/api/grades/roster/{offeringId}` | PROFESSOR | 获取班级名单 |
| POST | `/api/grades/submit` | PROFESSOR | 录入/修改成绩 |

### 成绩单

| 方法 | 路径 | 认证 | 说明 |
|---|---|---|---|
| GET | `/api/report-card` | STUDENT | 查看我的成绩单 |

### 用户管理(注册主任)

| 方法 | 路径 | 认证 | 说明 |
|---|---|---|---|
| GET | `/api/users` | REGISTRAR | 获取用户列表 |
| POST | `/api/users` | REGISTRAR | 创建用户 |
| PUT | `/api/users/{id}` | REGISTRAR | 更新用户信息 |
| PUT | `/api/users/{id}/status` | REGISTRAR | 激活/停用用户 |
| DELETE | `/api/users/{id}` | REGISTRAR | 删除用户(仅非活跃) |

### 注册管理(注册主任)

| 方法 | 路径 | 认证 | 说明 |
|---|---|---|---|
| GET | `/api/registration/status` | REGISTRAR | 获取注册阶段状态 |
| GET | `/api/registration/preview` | REGISTRAR | 预览关闭注册结果 |
| POST | `/api/registration/close` | REGISTRAR | 执行关闭注册 |

### 通知

| 方法 | 路径 | 认证 | 说明 |
|---|---|---|---|
| GET | `/api/notifications` | 需要 | 获取通知列表 |
| GET | `/api/notifications/unread-count` | 需要 | 获取未读通知数 |
| POST | `/api/notifications/{id}/read` | 需要 | 标记单条已读 |
| POST | `/api/notifications/read-all` | 需要 | 全部标为已读 |

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

| 用例 | 后端 | 前端 | 说明 |
|---|---|---|---|
| 7.1 Login 登录 | ✅ | ✅ | JWT 认证 + BCrypt |
| 7.2 Register for Courses 学生选课 | ✅ | ✅ | 课表管理 + 时间冲突检测 + 提交/保存 |
| 7.3 Select Courses to Teach 教授选课 | ✅ | ✅ | 教授选择教授开设 |
| 7.4 Submit Grades 提交成绩 | ✅ | ✅ | 成绩录入 + 通知学生 |
| 7.5 View Report Card 查看成绩单 | ✅ | ✅ | 学生成绩查询 |
| 7.6 Maintain Professor Information | ✅ | ✅ | 注册主任管理教授 |
| 7.7 Maintain Student Information | ✅ | ✅ | 注册主任管理学生 |
| 7.8 Close Registration 关闭注册 | ✅ | ✅ | 完整算法: commit/cancel + leveling + Billing 通知 |

### 已完成的基础设施

- ✅ 三层 B/S 架构 (Vue → Spring Boot REST → JPA → DB)
- ✅ 领域模型 (10 实体 + 8 枚举,覆盖第 9.3 节全部表)
- ✅ JPA Repository (10 个,含按学期/状态/学生等查询方法)
- ✅ 业务逻辑 (10 个 Service,覆盖全部 8 个用例)
- ✅ REST Controller (10 个,35+ 端点)
- ✅ JWT 认证 + BCrypt 密码哈希 + Spring Security 无状态配置
- ✅ CORS 配置 (放行前端开发服务器)
- ✅ 集中式审计日志 (AuditService, 记录登录/选课/成绩等操作)
- ✅ 全局异常处理 (统一 ErrorResponse 格式)
- ✅ 注册阶段状态机 (RegistrationPhase: open → add/drop → closed)
- ✅ 时间冲突检测工具 (TimeConflictUtil, ScheduleService/TeachingService 共用)
- ✅ H2 开发 / MySQL 生产 双 profile
- ✅ 种子数据 (3 类用户 + 5 课程 + 4 开设)
- ✅ Vue 3 前端 (9 个视图 + 1 个共用组件 + 路由守卫 + JWT 拦截器 + Element Plus)
- ✅ 前端路由守卫 (认证检查 + 角色 RBAC: STUDENT/PROFESSOR/REGISTRAR)
- ✅ 通知系统 (Notification 实体 + Service + Controller + 前端通知中心)
- ✅ 后端单元测试 (80+ 用例,覆盖 Schedule/Grade/Teaching/User/Auth/Registration)
- ✅ 前端单元测试 (46 用例,覆盖 Auth Store/API 拦截器/路由守卫)

## 已知限制

1. **前端需安装 Node.js** — 运行 `npm install` + `npm run dev` 即可启动。可使用 `login-test.html` 免安装测试。
2. **课程目录为模拟实现** — 使用本地 H2/MySQL 表模拟遗留 Ingres Catalog,后续需实现 Adapter + 缓存层(需求第 9.2 节)。
3. **无 HTTPS** — 生产环境需配置 TLS。

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
