package com.crs.common;

import com.crs.domain.SystemSetting;
import com.crs.repository.SystemSettingRepository;
import jakarta.annotation.PostConstruct;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 注册周期状态(持久化到数据库)。
 * 服务器重启后状态不会丢失。
 * <p>
 * 支持三种状态:
 * <ul>
 *   <li>{@code open} — 全面注册(允许新建/更新/删除课表、选课)</li>
 *   <li>{@code add-drop} — 增减课阶段(仅允许增删课,不能新建课表)</li>
 *   <li>{@code closed} — 注册关闭(不允许任何操作)</li>
 * </ul>
 * 状态转换: open → add-drop → closed
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegistrationPhase {

    private static final String PHASE_KEY = "registration.phase";
    private static final String DEFAULT_PHASE = "open";

    /** 所有合法的阶段值 */
    public static final String OPEN = "open";
    public static final String ADD_DROP = "add-drop";
    public static final String CLOSED = "closed";

    private static final Set<String> VALID_PHASES = Set.of(OPEN, ADD_DROP, CLOSED);

    /** 顺序: open(0) → add-drop(1) → closed(2) */
    private static final String[] PHASE_ORDER = {OPEN, ADD_DROP, CLOSED};

    private final SystemSettingRepository systemSettingRepository;

    private volatile String phase = DEFAULT_PHASE;

    @PostConstruct
    public void init() {
        // 从数据库加载注册周期状态
        // schema.sql 可能尚未执行完毕,允许降级到默认值
        try {
            systemSettingRepository.findBySettingKey(PHASE_KEY)
                    .ifPresent(setting -> {
                        String value = setting.getSettingValue();
                        if (VALID_PHASES.contains(value)) {
                            phase = value;
                        } else {
                            log.warn("数据库中的注册周期值 '{}' 不合法,使用默认值: {}",
                                    value, DEFAULT_PHASE);
                        }
                        log.info("从数据库加载注册周期状态: {}", phase);
                    });
        } catch (Exception e) {
            log.warn("数据库尚未就绪,注册周期状态使用内存默认值: {} ({})",
                    DEFAULT_PHASE, e.getMessage());
        }
    }

    public String getPhase() {
        return phase;
    }

    /**
     * 设置注册周期状态(线程安全)。
     *
     * @param newPhase 新状态,仅允许 "open"、"add-drop"、"closed"
     * @throws IllegalArgumentException 如果 newPhase 不在合法值范围内
     */
    public synchronized void setPhase(String newPhase) {
        if (newPhase == null || !VALID_PHASES.contains(newPhase)) {
            throw new IllegalArgumentException(
                    "非法的注册周期值: '" + newPhase + "',合法值为: " + VALID_PHASES);
        }
        String oldPhase = this.phase;
        this.phase = newPhase;
        // 持久化到数据库
        SystemSetting setting = systemSettingRepository.findBySettingKey(PHASE_KEY)
                .orElseGet(() -> {
                    SystemSetting s = new SystemSetting();
                    s.setSettingKey(PHASE_KEY);
                    return s;
                });
        setting.setSettingValue(newPhase);
        systemSettingRepository.save(setting);
        log.info("注册周期状态已更新并持久化: {} → {}", oldPhase, newPhase);
    }

    // ==================== 便捷判断方法 ====================

    /** 是否处于开放注册阶段(允许所有操作) */
    public boolean isOpen() {
        return OPEN.equals(getPhase());
    }

    /** 是否处于增减课阶段(仅允许 add/drop) */
    public boolean isAddDrop() {
        return ADD_DROP.equals(getPhase());
    }

    /** 是否已关闭注册(不允许任何操作) */
    public boolean isClosed() {
        return CLOSED.equals(getPhase());
    }

    // ==================== 阶段转换 ====================

    /**
     * 获取下一个合法阶段。
     * <p>
     * open → add-drop → closed
     *
     * @return 下一阶段名称,若已在 closed 则返回 closed
     */
    public String getNextPhase() {
        for (int i = 0; i < PHASE_ORDER.length - 1; i++) {
            if (PHASE_ORDER[i].equals(phase)) {
                return PHASE_ORDER[i + 1];
            }
        }
        return CLOSED; // 已经是 closed
    }

    /**
     * 是否可以前进到下一阶段。
     * closed 之后不能再前进。
     */
    public boolean canAdvance() {
        return !CLOSED.equals(phase);
    }

    /**
     * 推进到下一阶段(等价于 setPhase(getNextPhase()))。
     *
     * @return 推进后的新阶段
     * @throws IllegalStateException 如果当前已在 closed 阶段
     */
    public String advance() {
        if (!canAdvance()) {
            throw new IllegalStateException("注册已关闭,无法继续推进阶段");
        }
        String next = getNextPhase();
        setPhase(next);
        return next;
    }
}
