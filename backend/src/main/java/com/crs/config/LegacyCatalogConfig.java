package com.crs.config;

import com.crs.adapter.CachedCatalogAdapter;
import com.crs.adapter.CatalogAdapter;
import com.crs.adapter.DirectCatalogAdapter;
import com.crs.adapter.LegacyCatalogAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 遗留课程目录适配器配置。
 * <p>
 * 通过 {@code crs.catalog.legacy.enabled} 控制是否启用遗留 Ingres 适配器:
 * <ul>
 *   <li>{@code true} — 使用 {@link LegacyCatalogAdapter} + {@link CachedCatalogAdapter} 两级缓存</li>
 *   <li>{@code false} — 使用 {@link DirectCatalogAdapter} 直连本地 JPA(向后兼容)</li>
 * </ul>
 * <p>
 * 对应需求第 9.2 节: 可配置切换遗留/本地 Catalog 访问模式。
 */
@Slf4j
@Configuration
public class LegacyCatalogConfig {

    /**
     * 遗留模式: 通过 JdbcTemplate 查询遗留 Ingres + 两级缓存。
     * <p>
     * 当 crs.catalog.legacy.enabled=true 时激活。
     * CachedCatalogAdapter 装饰 LegacyCatalogAdapter,提供:
     * - L1 内存缓存(TTL 30s)
     * - L2 本地数据库物化视图模拟(TTL 150s)
     */
    @Bean("catalogAdapter")
    @ConditionalOnProperty(name = "crs.catalog.legacy.enabled", havingValue = "true")
    public CatalogAdapter legacyCachedCatalogAdapter(LegacyCatalogAdapter delegate) {
        log.info("启用遗留课程目录适配器: LegacyCatalogAdapter + CachedCatalogAdapter(两级缓存)");
        return new CachedCatalogAdapter(delegate);
    }

    /**
     * 非遗留模式: 直连本地 JPA Repository(向后兼容)。
     * <p>
     * 当 crs.catalog.legacy.enabled=false 时激活(默认行为)。
     */
    @Bean("catalogAdapter")
    @ConditionalOnProperty(name = "crs.catalog.legacy.enabled",
            havingValue = "false", matchIfMissing = true)
    public CatalogAdapter directCatalogAdapter(DirectCatalogAdapter delegate) {
        log.info("启用本地课程目录适配器: DirectCatalogAdapter(直连 JPA)");
        return delegate;
    }
}
