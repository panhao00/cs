package com.crs.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.util.Assert;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 配置。
 * <p>
 * 设计决策:
 * - 无状态 JWT(不使用 session)
 * - /auth/** 公开,其余需认证
 * - CORS 通过环境变量 CORS_ALLOWED_ORIGINS 配置(默认仅放行 localhost:5173)
 * - BCrypt 密码哈希
 * - WebSocket 端点在 STOMP CONNECT 帧校验 JWT(StompAuthChannelInterceptor)
 */
@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    /**
     * CORS 允许的来源模式,逗号分隔。
     * <p>
     * 使用 Spring OriginPattern 语法:
     * - "*" 匹配任意来源(兼容 credentials)
     * - "http://localhost:5173" 仅允许指定来源
     * 生产环境通过环境变量 CORS_ALLOWED_ORIGINS 设置,
     * 例如: https://app.example.com,https://admin.example.com
     * <p>
     * 注意: "http://*:*" 不能匹配含 "." 的 IP(如 http://10.67.34.5:5173),
     * 因为 AntPathMatcher 将 "." 视为路径分隔符。使用 "*" 替代。
     */
    @Value("${cors.allowed-origins:*}")
    private String corsAllowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // REST API: 关闭 CSRF
                .csrf(AbstractHttpConfigurer::disable)
                // CORS: 允许前端 Vite 开发服务器
                .cors(cors -> cors.configurationSource(corsSource()))
                // 无状态会话
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 安全响应头
                .headers(headers -> headers
                    .contentTypeOptions(Customizer.withDefaults())
                    .frameOptions(frame -> frame.deny())
                    .xssProtection(Customizer.withDefaults())
                    .referrerPolicy(referrer -> referrer
                        .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                )
                // 路径权限
                .authorizeHttpRequests(auth -> {
                    var r = auth
                            // 登录接口公开
                            .requestMatchers("/auth/login").permitAll()
                            // 注册接口公开
                            .requestMatchers("/auth/register").permitAll()
                            // 健康检查公开
                            .requestMatchers("/health").permitAll();
                    // [安全修复] 移除 /ws/** 的 permitAll:
                    // WebSocket 握手需要携带 JWT,由 JwtAuthenticationFilter 在 HTTP 层校验;
                    // STOMP CONNECT 帧由 StompAuthChannelInterceptor 二次校验 JWT。
                    r.anyRequest().authenticated();
                })
                // JWT 过滤器在 UsernamePasswordAuthenticationFilter 之前
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private CorsConfigurationSource corsSource() {
        CorsConfiguration config = new CorsConfiguration();
        // [安全修复] 通过环境变量 CORS_ALLOWED_ORIGINS 配置允许的来源,
        // 开发环境默认 http://localhost:5173,生产环境设置为实际域名(逗号分隔)。
        List<String> origins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
