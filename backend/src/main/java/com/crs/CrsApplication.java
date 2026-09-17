package com.crs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * 课程注册系统 (CRS) 启动入口。
 * <p>
 * 参考 AI 生成骨架,经人工核验与调整。
 */
@SpringBootApplication
@EnableJpaAuditing
public class CrsApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrsApplication.class, args);
    }
}
