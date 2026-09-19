# ============================================
# 多阶段构建: CRS 前端 (Vue 3 + Vite)
# ============================================

# ---------- Stage 1: Build ----------
FROM node:20-alpine AS builder

WORKDIR /build

# 缓存依赖
COPY package.json package-lock.json ./
RUN npm ci --prefer-offline

# 复制源码并构建
COPY . .
RUN npm run build

# ---------- Stage 2: Runtime ----------
FROM nginx:alpine

# 复制构建产物
COPY --from=builder /build/dist /usr/share/nginx/html

# 复制 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 暴露端口
EXPOSE 80

# 健康检查
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -qO- http://localhost/ || exit 1

# Nginx 前台运行
CMD ["nginx", "-g", "daemon off;"]
