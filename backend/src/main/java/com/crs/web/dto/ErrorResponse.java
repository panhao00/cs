package com.crs.web.dto;

import java.time.LocalDateTime;

/**
 * 统一错误响应。
 */
public record ErrorResponse(
    int status,
    String error,
    String message,
    LocalDateTime timestamp
) {
}
