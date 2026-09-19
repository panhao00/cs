# start.ps1 - 课程注册系统启动脚本
# ============================================
# 功能: 灵活启动服务 (支持多种启动模式)
# 使用: 
#   .\start.ps1                    # 启动所有服务 (后端+前端)
#   .\start.ps1 -BackendOnly       # 只启动后端
#   .\start.ps1 -FrontendOnly      # 只启动前端
#   .\start.ps1 -Build             # 强制重新构建后端
#   .\start.ps1 -Dev               # 开发模式 (MySQL数据库)
#   .\start.ps1 -Prod              # 生产模式 (MySQL数据库，需配置环境变量)
#   
# 停止服务: 关闭对应的 PowerShell 窗口即可
# ============================================

param(
    [switch]$BackendOnly,
    [switch]$FrontendOnly,
    [switch]$Build,
    [switch]$Dev,
    [switch]$Prod
)

$ErrorActionPreference = "Stop"

# ---------- 配置 ----------
$ProjectRoot = $PSScriptRoot
$BackendDir = Join-Path $ProjectRoot "backend"
$FrontendDir = Join-Path $ProjectRoot "frontend"

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

function Start-Backend {
    param([switch]$ShouldBuild, [string]$Profile)
    
    Write-Log "启动后端..."
    
    if (-not (Test-Path $BackendDir)) {
        Write-Log "后端目录不存在: $BackendDir" "ERROR"
        return
    }
    
    # 构建
    if ($ShouldBuild) {
        Write-Log "构建后端..."
        Push-Location $BackendDir
        try {
            & ".\mvnw.cmd" clean package -DskipTests
            if ($LASTEXITCODE -ne 0) {
                Write-Log "后端构建失败" "ERROR"
                return
            }
            Write-Log "后端构建成功" "OK"
        } finally {
            Pop-Location
        }
    }
    
    # 确定启动命令 - 始终使用 java -jar（避免 spring-boot:run 与 JaCoCo 的兼容性问题）
    $jarPath = Get-ChildItem -Path "$BackendDir\target" -Filter "*.jar" -ErrorAction SilentlyContinue | Where-Object { $_.Name -notlike "*-sources.jar" -and $_.Name -notlike "*-javadoc.jar" } | Select-Object -First 1
    
    if (-not $jarPath -or $ShouldBuild) {
        # 构建 jar
        Write-Log "构建后端 jar..."
        Push-Location $BackendDir
        try {
            & ".\mvnw.cmd" clean package -DskipTests
            if ($LASTEXITCODE -ne 0) {
                Write-Log "后端构建失败" "ERROR"
                return
            }
            Write-Log "后端构建成功" "OK"
        } finally {
            Pop-Location
        }
        $jarPath = Get-ChildItem -Path "$BackendDir\target" -Filter "*.jar" | Where-Object { $_.Name -notlike "*-sources.jar" -and $_.Name -notlike "*-javadoc.jar" } | Select-Object -First 1
    }
    
    if (-not $jarPath) {
        Write-Log "未找到jar文件" "ERROR"
        return
    }
    
    $startCmd = "java -jar `"$($jarPath.FullName)`""
    
    # 添加Profile参数
    if ($Profile) {
        $startCmd += " --spring.profiles.active=$Profile"
    }
    
    # 启动
    $workDir = "$BackendDir\target"
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$workDir'; $startCmd"
    Start-Sleep -Seconds 8
    
    # 检查
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:8080/api/health" -UseBasicParsing -TimeoutSec 5
        Write-Log "后端启动成功" "OK"
    } catch {
        Write-Log "后端可能仍在启动中..." "WARN"
    }
}

function Start-Frontend {
    Write-Log "启动前端..."
    
    if (-not (Test-Path $FrontendDir)) {
        Write-Log "前端目录不存在: $FrontendDir" "ERROR"
        return
    }
    
    # 检查 node_modules 是否存在
    $nodeModulesPath = Join-Path $FrontendDir "node_modules"
    if (-not (Test-Path $nodeModulesPath)) {
        Write-Log "前端依赖未安装，正在执行 npm install..." "WARN"
        Push-Location $FrontendDir
        try {
            & npm install
            if ($LASTEXITCODE -ne 0) {
                Write-Log "npm install 失败" "ERROR"
                return
            }
            Write-Log "前端依赖安装完成" "OK"
        } finally {
            Pop-Location
        }
    }
    
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$FrontendDir'; npm run dev -- --host"
    Start-Sleep -Seconds 3
    Write-Log "前端启动成功" "OK"
}

# ---------- 主逻辑 ----------
Write-Host ""
Write-Host "╔══════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║  Wylie College 课程注册系统 (CRS)               ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# 确定Profile
$profile = if ($Prod) { "prod" } elseif ($Dev) { "dev" } else { "" }

# 启动服务
if ($BackendOnly) {
    Start-Backend -ShouldBuild:$Build -Profile $profile
} elseif ($FrontendOnly) {
    Start-Frontend
} else {
    # 启动所有
    Start-Backend -ShouldBuild:$Build -Profile $profile
    Start-Frontend
}

# 完成信息
Write-Host ""
Write-Host "╔══════════════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║  服务启动完成                                   ║" -ForegroundColor Green
Write-Host "╚══════════════════════════════════════════════════╝" -ForegroundColor Green
Write-Host ""
Write-Log "本地访问: http://localhost:5173" "OK"
Write-Log "API 地址: http://localhost:8080/api" "OK"
Write-Host ""

