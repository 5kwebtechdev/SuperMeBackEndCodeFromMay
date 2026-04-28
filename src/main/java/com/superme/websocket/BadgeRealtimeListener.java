package com.superme.websocket;

import com.superme.dto.BadgeRealtimeEvent;
import com.superme.model.Badge;
import com.superme.service.BadgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class BadgeRealtimeListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final BadgeService badgeService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBadgeAwarded(BadgeAwardedEvent event) {

        Badge badge = event.getBadge();

        // 🔒 Safety: popup already acknowledged
        if (Boolean.TRUE.equals(badge.isPopupShown())) {
            log.debug("Popup already shown for badge {}", badge.getId());
            return;
        }

        String userId = badge.getUser().getId().toString();

        log.info("📡 Sending badge popup to user {}", userId);

        BadgeRealtimeEvent payload = BadgeRealtimeEvent.builder()
                .badgeId(badge.getId())
                .badgeName(badge.getBadgeType())
                .level(badge.getLevel())
                .title("🎉 New Badge Earned!")
                .message(
                        badgeService.getAchievementScreenText(
                                badge.getBadgeType(),
                                badge.getLevel()
                        )
                )
                .icon(badge.getIcon())
                .build();

        messagingTemplate.convertAndSendToUser(
                userId,
                "/queue/badge-popup",
                payload
        );
    }
}


