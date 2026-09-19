# ============================================
# CRS 课程注册系统 — 一键启动脚本
# ============================================
# 使用方法: 右键 -> 使用 PowerShell 运行
# 或者在 PowerShell 中执行: .\start-docker.ps1

param(
    [switch]$Stop,
    [switch]$Logs
)

$ErrorActionPreference = "Stop"

# 颜色输出函数
function Write-Status { param($Message) Write-Host "[INFO] $Message" -ForegroundColor Cyan }
function Write-Success { param($Message) Write-Host "[OK] $Message" -ForegroundColor Green }
function Write-Warning { param($Message) Write-Host "[WARN] $Message" -ForegroundColor Yellow }
function Write-Error { param($Message) Write-Host "[ERROR] $Message" -ForegroundColor Red }

# 检查 Docker 是否安装
function Test-Docker {
    try {
        $dockerVersion = docker --version 2>$null
        if ($dockerVersion) {
            Write-Success "Docker 已安装: $dockerVersion"
            return $true
        }
    } catch {
        Write-Error "Docker 未安装或未启动"
        Write-Host ""
        Write-Host "请先安装 Docker Desktop:" -ForegroundColor Yellow
        Write-Host "  https://docs.docker.com/desktop/install/windows-install/" -ForegroundColor Cyan
        Write-Host ""
        return $false
    }
}

# 检查 Docker Compose 是否可用
function Test-DockerCompose {
    try {
        $composeVersion = docker compose version 2>$null
        if ($composeVersion) {
            Write-Success "Docker Compose 已可用: $composeVersion"
            return $true
        }
    } catch {
        Write-Error "Docker Compose 不可用"
        return $false
    }
}

# 生成随机密钥
function New-SecretKey {
    param([int]$Length = 32)
    $chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*'
    $secret = ''
    for ($i = 0; $i -lt $Length; $i++) {
        $secret += $chars[(Get-Random -Maximum $chars.Length)]
    }
    return $secret
}

# 生成 Base64 密钥
function New-Base64Key {
    $bytes = New-Object Byte[] 32
    [System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
    return [Convert]::ToBase64String($bytes)
}

# 主函数
function Main {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host " CRS 课程注册系统 — Docker 部署" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""

    # 停止服务
    if ($Stop) {
        Write-Status "正在停止所有服务..."
        docker compose down
        Write-Success "所有服务已停止"
        return
    }

    # 查看日志
    if ($Logs) {
        Write-Status "正在查看日志..."
        docker compose logs -f
        return
    }

    # 检查 Docker
    if (-not (Test-Docker)) { return }
    if (-not (Test-DockerCompose)) { return }

    # 检查 .env 文件
    $envFile = ".env"
    $envExample = ".env.example"

    if (-not (Test-Path $envFile)) {
        Write-Warning ".env 文件不存在，正在从 .env.example 创建..."
        
        if (Test-Path $envExample) {
            Copy-Item $envExample $envFile
            Write-Success "已创建 .env 文件"
            
            # 生成密钥
            Write-Status "正在生成安全密钥..."
            $jwtSecret = New-SecretKey -Length 32
            $gradeKey = New-Base64Key
            
            # 更新 .env 文件
            $envContent = Get-Content $envFile -Raw
            $envContent = $envContent -replace 'JWT_SECRET=.*', "JWT_SECRET=$jwtSecret"
            $envContent = $envContent -replace 'GRADE_ENCRYPTION_KEY=.*', "GRADE_ENCRYPTION_KEY=$gradeKey"
            Set-Content $envFile $envContent
            
            Write-Success "已生成 JWT_SECRET 和 GRADE_ENCRYPTION_KEY"
        } else {
            Write-Error ".env.example 文件不存在，无法创建 .env"
            return
        }
    } else {
        Write-Success ".env 文件已存在"
    }

    # 检查必要的环境变量
    Write-Status "正在检查环境变量..."
    $envContent = Get-Content $envFile -Raw
    
    if ($envContent -match 'JWT_SECRET=change-me') {
        Write-Warning "JWT_SECRET 使用默认值，建议修改为强随机字符串"
    }
    
    if ($envContent -match 'GRADE_ENCRYPTION_KEY=$' -or $envContent -match 'GRADE_ENCRYPTION_KEY=\s*$') {
        Write-Warning "GRADE_ENCRYPTION_KEY 为空，正在生成..."
        $gradeKey = New-Base64Key
        $envContent = $envContent -replace 'GRADE_ENCRYPTION_KEY=.*', "GRADE_ENCRYPTION_KEY=$gradeKey"
        Set-Content $envFile $envContent
        Write-Success "已生成 GRADE_ENCRYPTION_KEY"
    }

    # 构建并启动服务
    Write-Status "正在构建并启动服务..."
    Write-Host ""
    Write-Host "首次构建可能需要几分钟，请耐心等待..." -ForegroundColor Yellow
    Write-Host ""

    docker compose up -d --build

    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Success "所有服务已成功启动！"
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Green
        Write-Host " 访问地址" -ForegroundColor Green
        Write-Host "========================================" -ForegroundColor Green
        Write-Host "  前端:     http://localhost" -ForegroundColor White
        Write-Host "  后端API:  http://localhost:8443/api" -ForegroundColor White
        Write-Host "  健康检查: http://localhost/health" -ForegroundColor White
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Green
        Write-Host " 默认测试账号" -ForegroundColor Green
        Write-Host "========================================" -ForegroundColor Green
        Write-Host "  学生:     student / student" -ForegroundColor White
        Write-Host "  教授:     professor / professor" -ForegroundColor White
        Write-Host "  注册主任: registrar / registrar" -ForegroundColor White
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Green
        Write-Host " 常用命令" -ForegroundColor Green
        Write-Host "========================================" -ForegroundColor Green
        Write-Host "  查看日志:    .\start-docker.ps1 -Logs" -ForegroundColor White
        Write-Host "  停止服务:    .\start-docker.ps1 -Stop" -ForegroundColor White
        Write-Host "  查看状态:    docker compose ps" -ForegroundColor White
        Write-Host "  重启服务:    docker compose restart" -ForegroundColor White
        Write-Host ""
    } else {
        Write-Error "服务启动失败，请检查错误信息"
    }
}

# 执行主函数
Main
