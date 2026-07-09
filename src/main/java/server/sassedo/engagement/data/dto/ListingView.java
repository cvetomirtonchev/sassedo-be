package server.sassedo.engagement.data.dto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import server.sassedo.promotion.common.ListingType;

import java.time.LocalDateTime;

/**
 * One unique view of a polymorphic listing. Uniqueness is per viewer, encoded in {@code viewerKey}:
 * {@code "U:<userId>"} for authenticated viewers and {@code "V:<visitorId>"} for anonymous ones.
 * The single unique index on (listingType, listingId, viewerKey) dedups both cases and avoids
 * MySQL's NULL-uniqueness pitfalls.
 */
@Getter
@Setter
@Entity
@Table(name = "listing_views",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_listing_view_viewer",
                        columnNames = {"listing_type", "listing_id", "viewer_key"})
        },
        indexes = {
                @Index(name = "idx_listing_view_listing", columnList = "listing_type, listing_id")
        })
public class ListingView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "listing_type", nullable = false)
    private ListingType listingType;

    @Column(name = "listing_id", nullable = false)
    private Long listingId;

    @Column(name = "viewer_key", nullable = false, length = 100)
    private String viewerKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
