package com.crs.security;

import com.crs.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT 令牌生成与校验。
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${crs.security.jwt-secret}") String secret,
            @Value("${crs.security.jwt-expiration-ms}") long expirationMs) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException(
                    "JWT 密钥不能为空，请设置环境变量 JWT_SECRET（至少 32 字符）");
        }
        if (secret.length() < 32) {
            throw new IllegalArgumentException(
                    "JWT 密钥长度至少 32 字符，当前长度: " + secret.length());
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /** 生成 JWT */
    public String generate(Long userId, String username, Role role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("role", role.name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /** 校验并解析 JWT;失败返回 null */
    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            log.debug("JWT 校验失败: {}", e.getMessage());
            return null;
        }
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
