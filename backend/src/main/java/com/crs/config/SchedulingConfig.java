package com.crs.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务调度配置。
 * <p>
 * 通过 {@code crs.scheduler.enabled} 控制(环境变量 {@code CRS_SCHEDULER_ENABLED}):
 * - 未配置(本地开发) → 默认启用
 * - docker 多实例部署: 仅实例 1 设置 {@code CRS_SCHEDULER_ENABLED=true},
 *   其余实例为 {@code false}, 避免所有实例重复执行计费重试等 {@code @Scheduled} 任务。
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "crs.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
