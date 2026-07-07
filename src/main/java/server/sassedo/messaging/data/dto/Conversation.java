package server.sassedo.messaging.data.dto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import server.sassedo.promotion.common.ListingType;

import java.time.LocalDateTime;

/**
 * A conversation between exactly two users, scoped to a single (polymorphic) post.
 * Participants are stored normalized (participant1Id = min, participant2Id = max) so the
 * unique constraint enforces a single conversation per user-pair per post. The title is
 * denormalized at creation time so the chat stays accessible even if the post is later
 * expired or deleted.
 */
@Getter
@Setter
@Entity
@Table(name = "conversations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_conversation_post_pair",
                        columnNames = {"listing_type", "listing_id", "participant1_id", "participant2_id"})
        },
        indexes = {
                @Index(name = "idx_conversation_participant1", columnList = "participant1_id, last_message_at"),
                @Index(name = "idx_conversation_participant2", columnList = "participant2_id, last_message_at")
        })
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "listing_type", nullable = false)
    private ListingType listingType;

    @Column(name = "listing_id", nullable = false)
    private Long listingId;

    @Column(name = "participant1_id", nullable = false)
    private Long participant1Id;

    @Column(name = "participant2_id", nullable = false)
    private Long participant2Id;

    @Column(length = 255)
    private String title;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.lastMessageAt == null) {
            this.lastMessageAt = this.createdAt;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
