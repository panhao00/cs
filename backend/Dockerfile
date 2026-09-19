# ============================================
# 多阶段构建: Course Registration System (CRS)
# ============================================

# ---------- Stage 1: Build ----------
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /build

# 缓存 Maven 依赖(利用 Docker 层缓存)
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw mvnw
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw dependency:go-offline -B

# 复制源码并构建
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw package -DskipTests -B

# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:21-jre

# 设置时区
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

# 创建非 root 用户
RUN groupadd --gid 1001 crs && \
    useradd --uid 1001 --gid 1001 --shell /bin/false --create-home crs

WORKDIR /app

# 安装 curl (健康检查需要)
RUN apt-get update && apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# 从构建阶段复制 JAR
COPY --from=builder /build/target/*.jar app.jar

# 创建日志目录并设置权限
RUN mkdir -p /app/logs && chown -R crs:crs /app

# 切换到非 root 用户
USER crs

# 暴露端口
EXPOSE 8080

# JVM 优化参数
ENV JAVA_OPTS="-XX:+UseG1GC \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseStringDeduplication \
    -XX:+OptimizeStringConcat \
    -Djava.security.egd=file:/dev/./urandom \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=Asia/Shanghai"

# 健康检查(每30秒)
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/health || exit 1

# 启动应用
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar ${0} ${@}"]
