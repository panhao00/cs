package com.crs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 课程注册系统 (CRS) 启动入口。
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
public class CrsApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrsApplication.class, args);
    }
}
