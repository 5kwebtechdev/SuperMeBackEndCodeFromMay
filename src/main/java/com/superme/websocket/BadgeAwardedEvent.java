package com.superme.websocket;

import com.superme.model.Badge;
import lombok.Getter;

@Getter
public class BadgeAwardedEvent {

    private final Badge badge;

    public BadgeAwardedEvent(Badge badge) {
        this.badge = badge;
    }
}

