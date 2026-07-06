package server.sassedo.promotion.data.dto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import server.sassedo.promotion.common.ListingType;
import server.sassedo.promotion.common.PromotionSource;
import server.sassedo.promotion.common.PromotionStatus;
import server.sassedo.promotion.common.PromotionType;

import java.time.LocalDateTime;

/**
 * Lifecycle + audit record for a single promotion of a single (polymorphic) listing.
 * Rows are never hard-deleted; promotion history is retained for auditing.
 */
@Getter
@Setter
@Entity
@Table(name = "promotions", indexes = {
        @Index(name = "idx_promo_status_end", columnList = "status, end_date"),
        @Index(name = "idx_promo_status_start", columnList = "status, start_date"),
        @Index(name = "idx_promo_listing", columnList = "listing_type, listing_id"),
        @Index(name = "idx_promo_owner", columnList = "owner_id")
})
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "listing_type", nullable = false)
    private ListingType listingType;

    @Column(name = "listing_id", nullable = false)
    private Long listingId;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "package_id")
    private Long packageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PromotionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PromotionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PromotionSource source = PromotionSource.PURCHASE;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "purchase_id")
    private Long purchaseId;

    @Column(nullable = false)
    private boolean pinned = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
