package com.crs.security;

import io.jsonwebtoken.Claims;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * STOMP 消息鉴权拦截器。
 * <p>
 * 场景: /ws 端点在 HTTP 层为 permitAll(SockJS 无法在握手阶段传 JWT),
 * 因此在此拦截器中对 STOMP 帧做二次鉴权:
 * - CONNECT: 从 connectHeaders 的 Authorization 头校验 JWT,成功后注入认证主体
 * - SUBSCRIBE: 校验订阅的 /topic/user/{userId}/... 频道归属为当前用户,防止越权订阅
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider tokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticateConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscribe(accessor);
        }

        return message;
    }

    /** CONNECT 帧: 校验 JWT,注入认证主体 */
    private void authenticateConnect(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("STOMP CONNECT 缺少 Authorization 头,拒绝连接");
            accessor.setUser(null);
            return;
        }

        String token = authHeader.substring(7);
        Claims claims = tokenProvider.parse(token);
        if (claims == null || claims.getSubject() == null) {
            log.warn("STOMP CONNECT JWT 校验失败,拒绝连接");
            accessor.setUser(null);
            return;
        }

        try {
            Long userId = Long.parseLong(claims.getSubject());
            String role = (String) claims.get("role");
            List<SimpleGrantedAuthority> authorities = role != null
                    ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    : List.of();
            var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            accessor.setUser(auth);
            log.debug("STOMP CONNECT 鉴权成功: userId={}, role={}", userId, role);
        } catch (NumberFormatException e) {
            log.warn("STOMP CONNECT subject 非法: {}", claims.getSubject());
            accessor.setUser(null);
        }
    }

    /** SUBSCRIBE 帧: 校验频道归属,防止越权订阅他人频道 */
    private void authorizeSubscribe(StompHeaderAccessor accessor) {
        var principal = accessor.getUser();
        if (principal == null || !(principal instanceof UsernamePasswordAuthenticationToken auth)
                || !(auth.getPrincipal() instanceof Long userId)) {
            log.warn("STOMP SUBSCRIBE 未认证,拒绝: {}", accessor.getDestination());
            accessor.setUser(null);
            return;
        }

        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }

        // 仅校验用户专属频道,通用 /topic/** 频道放行
        String prefix = "/topic/user/";
        if (!destination.startsWith(prefix)) {
            return;
        }

        String rest = destination.substring(prefix.length());
        int slash = rest.indexOf('/');
        String pathUserId = slash > 0 ? rest.substring(0, slash) : rest;

        try {
            long targetUserId = Long.parseLong(pathUserId);
            if (targetUserId != userId) {
                log.warn("STOMP SUBSCRIBE 越权: userId={} 尝试订阅 {} 的频道 {}",
                        userId, targetUserId, destination);
                accessor.setUser(null); // 置空认证使订阅被拒绝
            }
        } catch (NumberFormatException e) {
            log.warn("STOMP SUBSCRIBE 目标频道 ID 非法: {}", destination);
            accessor.setUser(null);
        }
    }
}
