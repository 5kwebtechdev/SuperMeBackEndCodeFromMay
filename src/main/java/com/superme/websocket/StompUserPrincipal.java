package com.superme.websocket;

import java.security.Principal;

public class StompUserPrincipal implements Principal {
    private final String name;
    public StompUserPrincipal(Long userId) {
        this.name = userId.toString();
    }
    public String getName() {
        return name;
    }
}

