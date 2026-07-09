package server.sassedo.engagement.data.dto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import server.sassedo.promotion.common.ListingType;

import java.time.LocalDateTime;

/**
 * A single user's "like" of a polymorphic listing (rental, roommate or apartment search).
 * Uniqueness of (userId, listingType, listingId) guarantees a listing can be favorited once per user.
 */
@Getter
@Setter
@Entity
@Table(name = "favorites",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_favorite_user_listing",
                        columnNames = {"user_id", "listing_type", "listing_id"})
        },
        indexes = {
                @Index(name = "idx_favorite_listing", columnList = "listing_type, listing_id"),
                @Index(name = "idx_favorite_user", columnList = "user_id")
        })
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "listing_type", nullable = false)
    private ListingType listingType;

    @Column(name = "listing_id", nullable = false)
    private Long listingId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
