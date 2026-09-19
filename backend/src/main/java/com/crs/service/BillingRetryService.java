package com.crs.service;

import com.crs.domain.Bill;
import com.crs.domain.BillStatus;
import com.crs.repository.BillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

/**
 * 账单重试服务: 从数据库中取 FAILED 状态的账单,按 nextRetryAt 调度重试发送。
 * <p>
 * 替代原有 RegistrationService 中的内存重试机制,实现持久化重试队列。
 * 备选流 Billing System Down: 按指数退避策略重试,超过 maxRetries 后标记为永久失败。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingRetryService {

    private final BillRepository billRepository;
    private final Random random = new Random();

    @Value("${crs.billing.retry-count:3}")
    private int maxRetries;

    @Value("${crs.billing.retry-delay-ms:60000}")
    private long retryDelayMs;

    /** 模拟失败开关: 开启时 sendBillingNotification 会随机抛异常,用于测试重试逻辑。生产环境务必为 false。 */
    @Value("${crs.billing.simulate-failure-enabled:false}")
    private boolean simulateFailureEnabled;

    @Scheduled(fixedDelayString = "${crs.billing.check-interval-ms:30000}")
    @Transactional
    public void processPendingBills() {
        LocalDateTime now = LocalDateTime.now();
        List<Bill> retryableBills = billRepository.findByStatusAndNextRetryAtBefore(
                BillStatus.FAILED, now);

        for (Bill bill : retryableBills) {
            if (bill.getRetryCount() >= bill.getMaxRetries()) {
                log.warn("Bill {} exceeded max retries ({}), marking as permanently failed",
                        bill.getId(), bill.getMaxRetries());
                bill.setStatus(BillStatus.FAILED);
                billRepository.save(bill);
                continue;
            }

            try {
                sendBillingNotification(bill);
                bill.setStatus(BillStatus.SENT);
                bill.setSentAt(LocalDateTime.now());
                bill.setRetryCount(bill.getRetryCount() + 1);
                log.info("Bill {} sent successfully on retry {}", bill.getId(), bill.getRetryCount());
            } catch (Exception e) {
                bill.setRetryCount(bill.getRetryCount() + 1);
                bill.setNextRetryAt(now.plusSeconds((retryDelayMs * bill.getRetryCount()) / 1000));
                bill.setLastError(e.getMessage());
                log.warn("Bill {} retry {} failed: {}", bill.getId(), bill.getRetryCount(), e.getMessage());
            }
            billRepository.save(bill);
        }
    }

    private void sendBillingNotification(Bill bill) {
        // 模拟 Billing 系统调用: 仅在 simulate-failure-enabled=true 时随机失败(用于测试重试逻辑)
        if (simulateFailureEnabled && random.nextInt(100) < 20) {
            throw new RuntimeException("Billing system unavailable (simulated)");
        }
        log.info("Billing notification sent for bill {}: student={}, amount={}",
                bill.getId(), bill.getStudentId(), bill.getAmount());
    }
}
