#!/bin/bash
# ============================================
# CRS 课程注册系统 — 一键启动脚本 (Linux/macOS)
# ============================================
# 使用方法: chmod +x start-docker.sh && ./start-docker.sh

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 打印函数
print_info() { echo -e "${CYAN}[INFO] $1${NC}"; }
print_ok() { echo -e "${GREEN}[OK] $1${NC}"; }
print_warn() { echo -e "${YELLOW}[WARN] $1${NC}"; }
print_error() { echo -e "${RED}[ERROR] $1${NC}"; }

# 生成随机密钥
generate_secret() {
    local length=${1:-32}
    openssl rand -base64 $length | tr -d '=/+' | head -c $length
}

# 生成 Base64 密钥
generate_base64_key() {
    openssl rand -base64 32
}

# 检查 Docker
check_docker() {
    if ! command -v docker &> /dev/null; then
        print_error "Docker 未安装"
        echo ""
        echo "请先安装 Docker:"
        echo "  - Docker Desktop: https://docs.docker.com/desktop/install/mac-install/"
        echo "  - 或使用包管理器安装 Docker Engine"
        echo ""
        return 1
    fi
    
    if ! docker info &> /dev/null; then
        print_error "Docker 未运行或当前用户无权限"
        echo "请启动 Docker 或将用户添加到 docker 组: sudo usermod -aG docker \$USER"
        return 1
    fi
    
    print_ok "Docker 已就绪: $(docker --version)"
    return 0
}

# 检查 Docker Compose
check_compose() {
    if docker compose version &> /dev/null; then
        print_ok "Docker Compose 已可用: $(docker compose version --short)"
        return 0
    elif command -v docker-compose &> /dev/null; then
        print_warn "使用旧版 docker-compose，请考虑升级到 Docker Compose V2"
        alias docker-compose="docker compose"
        return 0
    else
        print_error "Docker Compose 不可用"
        return 1
    fi
}

# 停止服务
stop_services() {
    print_info "正在停止所有服务..."
    docker compose down
    print_ok "所有服务已停止"
}

# 查看日志
view_logs() {
    print_info "正在查看日志..."
    docker compose logs -f
}

# 主函数
main() {
    echo ""
    echo -e "${CYAN}========================================${NC}"
    echo -e "${CYAN} CRS 课程注册系统 — Docker 部署${NC}"
    echo -e "${CYAN}========================================${NC}"
    echo ""

    # 处理参数
    case "${1:-}" in
        -Stop|--stop)
            stop_services
            return
            ;;
        -Logs|--logs)
            view_logs
            return
            ;;
        -h|--help)
            echo "使用方法: $0 [选项]"
            echo ""
            echo "选项:"
            echo "  -Stop, --stop    停止所有服务"
            echo "  -Logs, --logs    查看服务日志"
            echo "  -h, --help       显示帮助信息"
            echo ""
            echo "不带参数运行将启动所有服务"
            return
            ;;
    esac

    # 检查环境
    check_docker || return 1
    check_compose || return 1

    # 检查 .env 文件
    ENV_FILE=".env"
    ENV_EXAMPLE=".env.example"

    if [ ! -f "$ENV_FILE" ]; then
        print_warn ".env 文件不存在，正在从 .env.example 创建..."
        
        if [ -f "$ENV_EXAMPLE" ]; then
            cp "$ENV_EXAMPLE" "$ENV_FILE"
            print_ok "已创建 .env 文件"
            
            # 生成密钥
            print_info "正在生成安全密钥..."
            JWT_SECRET=$(generate_secret 32)
            GRADE_KEY=$(generate_base64_key)
            
            # 更新 .env 文件
            sed -i.bak "s|JWT_SECRET=.*|JWT_SECRET=$JWT_SECRET|" "$ENV_FILE"
            sed -i.bak "s|GRADE_ENCRYPTION_KEY=.*|GRADE_ENCRYPTION_KEY=$GRADE_KEY|" "$ENV_FILE"
            rm -f "$ENV_FILE.bak"
            
            print_ok "已生成 JWT_SECRET 和 GRADE_ENCRYPTION_KEY"
        else
            print_error ".env.example 文件不存在，无法创建 .env"
            return 1
        fi
    else
        print_ok ".env 文件已存在"
    fi

    # 检查必要的环境变量
    print_info "正在检查环境变量..."
    
    if grep -q "JWT_SECRET=change-me" "$ENV_FILE"; then
        print_warn "JWT_SECRET 使用默认值，建议修改为强随机字符串"
    fi
    
    if grep -q "GRADE_ENCRYPTION_KEY=$" "$ENV_FILE" || grep -q "GRADE_ENCRYPTION_KEY= *$" "$ENV_FILE"; then
        print_warn "GRADE_ENCRYPTION_KEY 为空，正在生成..."
        GRADE_KEY=$(generate_base64_key)
        sed -i.bak "s|GRADE_ENCRYPTION_KEY=.*|GRADE_ENCRYPTION_KEY=$GRADE_KEY|" "$ENV_FILE"
        rm -f "$ENV_FILE.bak"
        print_ok "已生成 GRADE_ENCRYPTION_KEY"
    fi

    # 构建并启动服务
    print_info "正在构建并启动服务..."
    echo ""
    echo -e "${YELLOW}首次构建可能需要几分钟，请耐心等待...${NC}"
    echo ""

    docker compose up -d --build

    if [ $? -eq 0 ]; then
        echo ""
        print_ok "所有服务已成功启动！"
        echo ""
        echo -e "${GREEN}========================================${NC}"
        echo -e "${GREEN} 访问地址${NC}"
        echo -e "${GREEN}========================================${NC}"
        echo "  前端:     http://localhost"
        echo "  后端API:  http://localhost:8443/api"
        echo "  健康检查: http://localhost/health"
        echo ""
        echo -e "${GREEN}========================================${NC}"
        echo -e "${GREEN} 默认测试账号${NC}"
        echo -e "${GREEN}========================================${NC}"
        echo "  学生:     student / student"
        echo "  教授:     professor / professor"
        echo "  注册主任: registrar / registrar"
        echo ""
        echo -e "${GREEN}========================================${NC}"
        echo -e "${GREEN} 常用命令${NC}"
        echo -e "${GREEN}========================================${NC}"
        echo "  查看日志:    $0 -Logs"
        echo "  停止服务:    $0 -Stop"
        echo "  查看状态:    docker compose ps"
        echo "  重启服务:    docker compose restart"
        echo ""
    else
        print_error "服务启动失败，请检查错误信息"
        return 1
    fi
}

# 执行主函数
main "$@"
