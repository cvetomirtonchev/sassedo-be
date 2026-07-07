package server.sassedo.messaging.data.dto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A single message within a {@link Conversation}. {@code isRead} tracks whether the
 * recipient has seen the message and drives the unread badge/count.
 */
@Getter
@Setter
@Entity
@Table(name = "messages", indexes = {
        @Index(name = "idx_message_conversation_created", columnList = "conversation_id, created_at")
})
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
