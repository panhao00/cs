package com.crs.service;

import com.crs.domain.Enrollment;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 异步 Billing 通知服务: 将 Thread.sleep 移出 @Transactional 方法。
 */
@Slf4j
@Service
public class BillingAsyncService {

    private final int retryCount = 3;
    private final long retryDelayMs = 1000;

    /** 模拟失败开关: 开启时 30% 概率随机抛异常,用于测试重试逻辑。生产环境务必为 false。 */
    @Value("${crs.billing.simulate-failure-enabled:false}")
    private boolean simulateFailureEnabled;

    /**
     * 异步通知 Billing 系统(带重试)。
     *
     * @param semester    学期
     * @param enrollments 选课记录
     * @return CompletableFuture<Boolean> 是否成功
     */
    @Async
    public CompletableFuture<Boolean> notifyBillingSystemAsync(
            String semester, List<Enrollment> enrollments) {

        Random random = new Random();

        for (int attempt = 1; attempt <= retryCount; attempt++) {
            try {
                log.info("通知 Billing 系统: attempt={}/{}, semester={}, 共 {} 条选课记录",
                        attempt, retryCount, semester, enrollments.size());

                // 按学生分组统计
                Map<Long, Long> studentCourseCount = enrollments.stream()
                        .collect(Collectors.groupingBy(
                                Enrollment::getStudentId,
                                Collectors.counting()));

                log.info("Billing 通知: {} 名学生, 选课记录总计 {} 条",
                        studentCourseCount.size(), enrollments.size());

                // 模拟失败: 仅在 simulate-failure-enabled=true 时 30% 概率随机失败,用于测试重试逻辑
                if (simulateFailureEnabled && random.nextInt(100) < 30) {
                    throw new RuntimeException("Billing System 暂时不可用 (模拟失败)");
                }

                log.info("Billing 通知成功: attempt={}", attempt);
                return CompletableFuture.completedFuture(true);

            } catch (Exception e) {
                log.warn("Billing 通知失败: attempt={}/{}, 错误: {}",
                        attempt, retryCount, e.getMessage());

                if (attempt < retryCount) {
                    try {
                        log.info("等待 {}ms 后重试...", retryDelayMs);
                        TimeUnit.MILLISECONDS.sleep(retryDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("重试等待被中断", ie);
                        return CompletableFuture.completedFuture(false);
                    }
                }
            }
        }

        log.error("Billing 通知最终失败: 已重试 {} 次", retryCount);
        return CompletableFuture.completedFuture(false);
    }
}
