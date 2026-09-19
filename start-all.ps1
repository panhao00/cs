# start-all.ps1 - 课程注册系统一键启动脚本
# ============================================
# 功能: 启动所有服务 (MySQL + 后端 + 前端)
# 使用: 
#   .\start-all.ps1                    # 启动所有服务
#   .\start-all.ps1 -SkipMySQL         # 跳过 MySQL 启动
#   .\start-all.ps1 -SkipBackend       # 跳过后端启动
#   .\start-all.ps1 -SkipFrontend      # 跳过前端启动
#   .\start-all.ps1 -Build             # 先构建后端再启动
#   
# 停止服务: 关闭对应的 PowerShell 窗口即可
# ============================================

param(
    [switch]$SkipMySQL,
    [switch]$SkipBackend,
    [switch]$SkipFrontend,
    [switch]$Build
)

$ErrorActionPreference = "Stop"

# ---------- 配置 ----------
$ProjectRoot = $PSScriptRoot
$BackendDir = Join-Path $ProjectRoot "backend"
$FrontendDir = Join-Path $ProjectRoot "frontend"

# MySQL 配置 (可通过环境变量覆盖)
$MySQLBin = if ($env:MYSQL_BIN) { $env:MYSQL_BIN } else { "C:\Program Files\MySQL\MySQL Server 8.4\bin" }
$MySQLData = if ($env:MYSQL_DATA) { $env:MYSQL_DATA } else { "C:\ProgramData\MySQL\MySQL Server 8.4\Data" }

# ---------- 辅助函数 ----------
function Write-Log {
    param([string]$Message, [string]$Level = "INFO")
    $timestamp = Get-Date -Format "HH:mm:ss"
    $color = switch ($Level) {
        "OK"    { "Green" }
        "WARN"  { "Yellow" }
        "ERROR" { "Red" }
        default { "Cyan" }
    }
    Write-Host "[$timestamp][$Level] $Message" -ForegroundColor $color
}

function Test-Port {
    param([int]$Port)
    try {
        $tcp = New-Object System.Net.Sockets.TcpClient
        $tcp.Connect("127.0.0.1", $Port)
        $tcp.Close()
        return $true
    } catch {
        return $false
    }
}

# ---------- 开始 ----------
Write-Host ""
Write-Host "╔══════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║  Wylie College 课程注册系统 (CRS)               ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# 1. 启动 MySQL
if (-not $SkipMySQL) {
    Write-Log "检查 MySQL 服务..."
    
    $mysqlProcess = Get-Process -Name mysqld -ErrorAction SilentlyContinue
    if ($mysqlProcess) {
        Write-Log "MySQL 已在运行" "OK"
    } else {
        if (-not (Test-Path "$MySQLBin\mysqld.exe")) {
            Write-Log "未找到 MySQL: $MySQLBin\mysqld.exe" "WARN"
            Write-Log "请安装 MySQL 或设置环境变量 MYSQL_BIN" "WARN"
        } else {
            Write-Log "启动 MySQL..."
            Start-Process -FilePath "$MySQLBin\mysqld.exe" -ArgumentList "--defaults-file=`"$MySQLData\..\my.ini`"", "--basedir=`"$MySQLBin`"", "--datadir=`"$MySQLData`"", "--port=3306" -WindowStyle Hidden
            
            $maxWait = 30
            $waited = 0
            while ($waited -lt $maxWait) {
                Start-Sleep -Seconds 1
                $waited++
                $mysqlProcess = Get-Process -Name mysqld -ErrorAction SilentlyContinue
                if ($mysqlProcess) {
                    Write-Log "MySQL 启动成功" "OK"
                    break
                }
            }
            if ($waited -ge $maxWait) {
                Write-Log "MySQL 启动超时" "ERROR"
            }
        }
    }
    
    # 等待端口就绪
    Start-Sleep -Seconds 2
    if (Test-Port 3306) {
        Write-Log "MySQL 端口 3306 已就绪" "OK"
    } else {
        Write-Log "MySQL 端口 3306 未就绪" "WARN"
    }
    
    # 创建数据库
    Write-Log "检查 crs 数据库..."
    if (Test-Path "$MySQLBin\mysql.exe") {
        # 使用 -ErrorAction SilentlyContinue 忽略 NativeCommandError
        $ErrorActionPreference_backup = $ErrorActionPreference
        $ErrorActionPreference = "SilentlyContinue"
        & "$MySQLBin\mysql.exe" -u root -proot -e "CREATE DATABASE IF NOT EXISTS crs CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>$null
        $ErrorActionPreference = $ErrorActionPreference_backup
        Write-Log "crs 数据库就绪" "OK"
    }
}

# 2. 启动后端
if (-not $SkipBackend) {
    Write-Log "启动后端..."
    
    if (-not (Test-Path $BackendDir)) {
        Write-Log "后端目录不存在: $BackendDir" "ERROR"
    } else {
        # 构建 jar（确保 jar 存在）
        $jarPath = Get-ChildItem -Path "$BackendDir\target" -Filter "*.jar" -ErrorAction SilentlyContinue | Where-Object { $_.Name -notlike "*-sources.jar" -and $_.Name -notlike "*-javadoc.jar" } | Select-Object -First 1
        
        if (-not $jarPath -or $Build) {
            Write-Log "构建后端 jar..."
            Push-Location $BackendDir
            try {
                & ".\mvnw.cmd" clean package -DskipTests
                if ($LASTEXITCODE -ne 0) {
                    Write-Log "后端构建失败" "ERROR"
                } else {
                    Write-Log "后端构建成功" "OK"
                }
            } finally {
                Pop-Location
            }
            $jarPath = Get-ChildItem -Path "$BackendDir\target" -Filter "*.jar" | Where-Object { $_.Name -notlike "*-sources.jar" -and $_.Name -notlike "*-javadoc.jar" } | Select-Object -First 1
        }
        
        if (-not $jarPath) {
            Write-Log "未找到jar文件" "ERROR"
        } else {
            # 启动后端（使用 java -jar，避免 spring-boot:run 与 JaCoCo 的兼容性问题）
            $jarFullPath = $jarPath.FullName
            Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$BackendDir\target'; java -jar `"$jarFullPath`""
            Start-Sleep -Seconds 8
            
            # 检查后端
            try {
                $r = Invoke-WebRequest -Uri "http://localhost:8080/api/health" -UseBasicParsing -TimeoutSec 5
                Write-Log "后端启动成功" "OK"
            } catch {
                Write-Log "后端可能仍在启动中..." "WARN"
            }
        }
    }
}

# 3. 启动前端
if (-not $SkipFrontend) {
    Write-Log "启动前端..."
    
    if (-not (Test-Path $FrontendDir)) {
        Write-Log "前端目录不存在: $FrontendDir" "ERROR"
    } else {
        # 检查 node_modules 是否存在
        $nodeModulesPath = Join-Path $FrontendDir "node_modules"
        if (-not (Test-Path $nodeModulesPath)) {
            Write-Log "前端依赖未安装，正在执行 npm install..." "WARN"
            Push-Location $FrontendDir
            try {
                & npm install
                if ($LASTEXITCODE -ne 0) {
                    Write-Log "npm install 失败" "ERROR"
                } else {
                    Write-Log "前端依赖安装完成" "OK"
                }
            } finally {
                Pop-Location
            }
        }
        
        Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$FrontendDir'; npm run dev -- --host"
        Start-Sleep -Seconds 3
        Write-Log "前端启动成功" "OK"
    }
}

# 4. 完成
Write-Host ""
Write-Host "╔══════════════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║  所有服务已启动                                 ║" -ForegroundColor Green
Write-Host "╚══════════════════════════════════════════════════╝" -ForegroundColor Green
Write-Host ""
Write-Log "本地访问: http://localhost:5173" "OK"
Write-Log "API 地址: http://localhost:8080/api" "OK"
Write-Host ""



