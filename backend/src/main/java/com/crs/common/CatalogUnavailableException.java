package com.crs.common;

import lombok.Getter;

/**
 * 课程目录系统不可用异常。
 * <p>
 * 当遗留课程目录适配器在重试后仍无法访问时抛出,
 * 对应需求备选流 "Course Catalog System Unavailable"。
 * 上层服务捕获后转换为 503 Service Unavailable 响应。
 */
@Getter
public class CatalogUnavailableException extends RuntimeException {

    private final int retryCount;
    private final String errorMessage;

    public CatalogUnavailableException(int retryCount, String errorMessage, Throwable cause) {
        super("课程目录系统在 " + retryCount + " 次重试后仍不可用: " + errorMessage, cause);
        this.retryCount = retryCount;
        this.errorMessage = errorMessage;
    }
}
