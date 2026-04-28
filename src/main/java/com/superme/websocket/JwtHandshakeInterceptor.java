package com.superme.websocket;

import com.superme.util.UserJwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
@Slf4j
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {

        // Extract the token from the query parameters
        String query = request.getURI().getQuery();
        if (query != null && query.contains("token=")) {
            String token = query.split("token=")[1];
            if (token.contains("&")) {
                token = token.split("&")[0];
            }

            // Validate the token and extract user information
            try {
                Long userId = UserJwtUtil.getUserIdFromToken(token);
                attributes.put("userId", userId); // Store userId in session attributes
                return true; // Allow the handshake
            } catch (Exception e) {
                // Token validation failed
                return false;
            }
        }

        // Reject the handshake if no token is provided
        return false;
    }


    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {}
}

