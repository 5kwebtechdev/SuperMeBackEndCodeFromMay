package com.superme.websocket;


import com.superme.util.UserJwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class JwtStompChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) return message;

        // 🔑 Authenticate ONLY on CONNECT
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            List<String> authHeaders = accessor.getNativeHeader("Authorization");

            if (authHeaders == null || authHeaders.isEmpty()) {
                throw new MessagingException("Missing Authorization header");
            }

            String token = authHeaders.get(0).replace("Bearer ", "");

            Long userId = UserJwtUtil.getUserIdFromToken(token);

            accessor.setUser(new StompUserPrincipal(userId));

            log.info("✅ WS CONNECT authenticated userId={}", userId);
        }

        return message;
    }
}

