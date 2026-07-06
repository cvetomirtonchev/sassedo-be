package server.sassedo.promotion.data.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;
import server.sassedo.promotion.common.PromotionType;

import java.time.LocalDateTime;

/**
 * Denormalized promotion state carried on every listing row so browse/search can sort by
 * tier with a single indexed query instead of joining the polymorphic {@code promotions}
 * table. Kept in sync by the activation logic and the {@code PromotionScheduler}; the
 * {@code Promotion} table remains the source of truth.
 */
@Getter
@Setter
@Embeddable
public class PromotionState {

    /** 0 = STANDARD, 1 = PROMOTED, 2 = FEATURED. Numeric so it sorts and indexes correctly. */
    @Column(name = "promotion_priority", nullable = false)
    private int promotionPriority = 0;

    /** Intra-tier ordering key: newest active promotion first. Null for standard listings. */
    @Column(name = "promotion_activated_at")
    private LocalDateTime promotionActivatedAt;

    @Column(name = "promoted_until")
    private LocalDateTime promotedUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "promotion_type")
    private PromotionType promotionType;

    @Column(name = "active_promotion_id")
    private Long activePromotionId;

    @Column(name = "pinned", nullable = false)
    private boolean pinned = false;

    public void applyActive(PromotionType type, Long promotionId, LocalDateTime activatedAt,
                            LocalDateTime until, boolean pinned) {
        this.promotionType = type;
        this.promotionPriority = type.getPriority();
        this.activePromotionId = promotionId;
        this.promotionActivatedAt = activatedAt;
        this.promotedUntil = until;
        this.pinned = pinned;
    }

    public void reset() {
        this.promotionType = null;
        this.promotionPriority = 0;
        this.activePromotionId = null;
        this.promotionActivatedAt = null;
        this.promotedUntil = null;
        this.pinned = false;
    }
}
