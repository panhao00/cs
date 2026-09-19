package com.crs;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 应用上下文加载测试。
 */
@SpringBootTest
@ActiveProfiles("test")
class CrsApplicationTests {

    @Test
    void contextLoads() {
        // 验证 Spring 上下文能正常启动
    }
}
