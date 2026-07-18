package server.sassedo.report.data.dto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import server.sassedo.promotion.common.ListingType;

import java.time.LocalDateTime;

/**
 * A single user's report of a polymorphic listing (rental, roommate or apartment search).
 * Uniqueness of (reporterUserId, listingType, listingId) guarantees a listing can be reported
 * once per user, mirroring the {@code Favorite} entity.
 */
@Getter
@Setter
@Entity
@Table(name = "listing_reports",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_report_user_listing",
                        columnNames = {"reporter_user_id", "listing_type", "listing_id"})
        },
        indexes = {
                @Index(name = "idx_report_listing", columnList = "listing_type, listing_id"),
                @Index(name = "idx_report_status", columnList = "status")
        })
public class ListingReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporter_user_id", nullable = false)
    private Long reporterUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "listing_type", nullable = false)
    private ListingType listingType;

    @Column(name = "listing_id", nullable = false)
    private Long listingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportReason reason;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ReportStatus.OPEN;
        }
    }
}
