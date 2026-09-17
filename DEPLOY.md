# 跨 PC 部署指南(U 盘离线运行)

本文档说明如何将 CRS 课程注册系统打包到 U 盘,在**任意 Windows PC**上免安装即插即用。

## 一、推荐方案:fat jar + JDK

项目在 `runtime/jdk` 内置便携版 JDK 21,后端打包为 fat jar(`backend/target/*.jar`)。
**目标 PC 只需插入 U 盘,无需安装任何软件**,即可运行后端。

```
f:\cs\
├── runtime\
│   └── jdk\                       # 便携版 JDK 21(约 326 MB)
│       └── bin\java.exe
├── backend\
│   └── target\
│       └── course-registration-system-0.1.0.jar   # 预构建 fat jar(约 53 MB)
├── frontend\                       # 可选:前端源码 + node_modules
├── start.ps1                       # 一键启动脚本
└── DEPLOY.md                       # 本文档
```

**关键:** `start.ps1` 通过 `$PSScriptRoot` 推导路径,设置 `JAVA_HOME` 到 `runtime/jdk`,完全不依赖系统 PATH。

## 二、首次打包步骤(在有 Java/Node 的开发机执行)

### 前置条件
- Windows 10/11,PowerShell 5.1+
- JDK 21+ 已安装
- Maven 3.6+(或用项目自带的 `tools/apache-maven-3.9.9/`)
- Node.js 18+(仅前端需要)

### 步骤

```powershell
cd f:\cs

# 1. 创建 runtime 目录
New-Item -Path runtime\jdk -ItemType Directory -Force

# 2. 复制 JDK 到 runtime/jdk(从系统 JAVA_HOME)
robocopy $env:JAVA_HOME runtime\jdk /E /NFL /NDL /XA:S

# 3. 构建 fat jar(用 runtime/jdk,确保目标 PC 兼容)
$env:JAVA_HOME = 'f:\cs\runtime\jdk'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
cd backend
f:\cs\tools\apache-maven-3.9.9\bin\mvn.cmd clean package -DskipTests
cd ..
# 产物: backend\target\course-registration-system-0.1.0.jar (约 53 MB)

# 4. (可选)安装前端依赖,供前端开发/调试
cd frontend
npm install --registry https://registry.npmmirror.com
cd ..
```

完成后,U 盘总占用约 400 MB(jdk 326 + jar 53 + 源码),完全离线。

## 三、目标 PC 运行(免安装,推荐)

### 唯一前置条件
- Windows 10/11(64 位)
- PowerShell 5.1+(系统自带)

### 步骤

1. 插入 U 盘,记下盘符(假设 `E:`)
2. 打开 PowerShell:
   - `Win+X` → Windows PowerShell
   - 或资源管理器进入 `E:\cs`,地址栏输入 `powershell` 回车
3. 解除执行策略(一次性):
   ```powershell
   Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
   ```
4. 启动后端(推荐 fat jar 模式,秒级启动):
   ```powershell
   cd E:\cs
   .\start.ps1 -Jar -BackendOnly
   ```
5. 访问:
   - 健康检查: http://localhost:8080/api/health
   - API 文档: http://localhost:8080/api/...

### 默认账号
| 用户名 | 密码 | 角色 |
|---|---|---|
| `registrar` | `registrar` | 注册主任 |
| `professor` | `professor` | 教授 |
| `student` | `student` | 学生 |

## 四、前端运行(可选)

前端需要 Node.js 18+。三种方式:

### 方式 A:目标 PC 已装 Node
```powershell
cd E:\cs\frontend
npm install --registry https://registry.npmmirror.com   # 首次
npm run dev
# 访问 http://localhost:5173
```

### 方式 B:打包便携 Node 到 runtime/node
```powershell
# 在开发机执行:
$url = 'https://nodejs.org/dist/v20.18.0/node-v20.18.0-win-x64.zip'
Invoke-WebRequest $url -OutFile runtime\node.zip
Expand-Archive runtime\node.zip -DestinationPath runtime\node-tmp
Move-Item runtime\node-tmp\node-v20.18.0-win-x64\* runtime\node\ -Force
Remove-Item runtime\node.zip, runtime\node-tmp -Recurse -Force
# 目标 PC 用 start.ps1 -FrontendOnly(自动用 runtime/node)
```

### 方式 C:仅用后端 API + 静态 login-test.html
项目根目录的 `login-test.html` 可直接在浏览器打开,测试登录和核心 API,无需 Node。

## 五、其他启动模式

```powershell
.\start.ps1 -Jar                  # fat jar 后端 + 新窗口前端
.\start.ps1 -Jar -BackendOnly     # 仅 fat jar 后端(最快)
.\start.ps1 -BackendOnly           # mvnw 后端(需联网下载 Maven)
.\start.ps1 -FrontendOnly         # 仅前端
.\start.ps1 -Build                # 构建 fat jar + 前端 dist
.\start.ps1 -InstallFrontend      # 仅 npm install
```

## 六、常见问题

### Q1. PowerShell 提示"无法加载脚本,未签名"
```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
```
`-Scope Process` 仅对当前会话有效,关闭窗口即恢复,无安全风险。

### Q2. 端口 8080 或 5173 被占用
编辑:
- 后端: `backend\src\main\resources\application.yml` → `server.port`
- 前端: `frontend\vite.config.js` → `server.port`
或停止占用进程:
```powershell
Get-NetTCPConnection -LocalPort 8080 | Select OwningProcess | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
```

### Q3. fat jar 启动报"找不到主类"
检查 jar 是否完整:
```powershell
& runtime\jdk\bin\jar.exe -tf backend\target\course-registration-system-0.1.0.jar | Select-String 'CrsApplication'
```
应能看到 `BOOT-INF/classes/com/crs/CrsApplication.class`。若缺失,需重新执行 `mvn package`。

### Q4. U 盘文件系统
FAT32 单文件上限 4 GB,本项目最大文件是 fat jar(53 MB),无超限风险。
但 exFAT/NTFS 的 I/O 性能更好,推荐将 U 盘格式化为 exFAT。

### Q5. JDK 架构不匹配
`runtime/jdk` 是 x64。若目标 PC 是 ARM64,需替换为 ARM64 版 JDK。

### Q6. 数据持久化
开发 profile 用 H2 内存库,重启后数据丢失(种子数据自动重建)。如需持久化:
- 编辑 `backend/src/main/resources/application.yml`,将 `spring.profiles.active` 改为 `prod`
- 设置环境变量 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 指向本地 MySQL
- 或修改 `application-dev.yml` 将 H2 改为文件模式:`jdbc:h2:file:./data/crs`

### Q7. Maven Wrapper 下载失败(404)
`backend/.mvn/wrapper/maven-wrapper.properties` 中的 `distributionUrl` 已修复为具体版本 3.9.9。
若仍失败,可直接用 `f:\cs\tools\apache-maven-3.9.9\bin\mvn.cmd` 替代 `.\mvnw.cmd`。

## 七、体积优化(可选)

- 删除 JDK 的 `lib/src.zip`(约 50 MB):`Remove-Item runtime\jdk\lib\src.zip`
- 删除 JDK 的 `jmods/`(若不用 jlink,约 80 MB)
- 删除 backend 源码:仅保留 `backend/target/*.jar` 即可运行
- 最终最小化:runtime/jdk(精简后约 150 MB)+ fat jar(53 MB)≈ 200 MB
