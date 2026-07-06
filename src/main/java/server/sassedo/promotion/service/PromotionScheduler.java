package server.sassedo.promotion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Keeps promotion lifecycle in sync: activates scheduled promotions whose start has
 * arrived and expires active promotions whose end has passed (downgrading the listing back
 * to Standard). Idempotent and bulk-query driven; runs every {@code sassedo.promotions.sweep-ms}
 * (default 60s).
 */
@Component
@Log4j2
@RequiredArgsConstructor
public class PromotionScheduler {

    private final PromotionService promotionService;

    @Scheduled(fixedDelayString = "${sassedo.promotions.sweep-ms:60000}",
            initialDelayString = "${sassedo.promotions.sweep-initial-ms:30000}")
    public void sweep() {
        LocalDateTime now = LocalDateTime.now();
        int activated = promotionService.activateScheduled(now);
        int expired = promotionService.expireOverdue(now);
        if (activated > 0 || expired > 0) {
            log.info("Promotion sweep: activated={}, expired={}", activated, expired);
        }
    }
}
