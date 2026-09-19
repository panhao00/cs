package com.crs.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 定时任务开关验证:
 * - 未配置(本地开发) → 调度启用
 * - CRS_SCHEDULER_ENABLED=true → 调度启用
 * - CRS_SCHEDULER_ENABLED=false → 调度禁用(多实例仅实例1执行)
 */
class SchedulingConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(SchedulingConfig.class);

    @Test
    @DisplayName("未配置开关时默认启用调度")
    void schedulerEnabledByDefault() {
        runner.run(ctx ->
                assertThat(ctx).hasSingleBean(ScheduledAnnotationBeanPostProcessor.class));
    }

    @Test
    @DisplayName("CRS_SCHEDULER_ENABLED=true 时启用调度")
    void schedulerEnabledWhenTrue() {
        runner.withPropertyValues("crs.scheduler.enabled=true")
                .run(ctx -> assertThat(ctx).hasSingleBean(ScheduledAnnotationBeanPostProcessor.class));
    }

    @Test
    @DisplayName("CRS_SCHEDULER_ENABLED=false 时禁用调度")
    void schedulerDisabledWhenFalse() {
        runner.withPropertyValues("crs.scheduler.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(ScheduledAnnotationBeanPostProcessor.class));
    }

    @Test
    @DisplayName("SchedulingConfig 使用条件注解, 属性名正确")
    void conditionalAnnotationPresent() {
        ConditionalOnProperty annotation = SchedulingConfig.class
                .getAnnotation(ConditionalOnProperty.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.name()).containsExactly("crs.scheduler.enabled");
        assertThat(annotation.havingValue()).isEqualTo("true");
        assertThat(annotation.matchIfMissing()).isTrue();
    }
}
