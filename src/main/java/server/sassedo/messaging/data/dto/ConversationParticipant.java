package server.sassedo.messaging.data.dto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Per-user state for a {@link Conversation}. Denormalizes the unread counter and read position so the
 * inbox badge and per-conversation unread count are answered from indexed rows instead of scanning
 * the {@code messages} table on every poll. Exactly one row exists per (conversation, participant).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@IdClass(ConversationParticipantId.class)
@Table(name = "conversation_participants", indexes = {
        @Index(name = "idx_participant_user_unread", columnList = "user_id, unread_count")
})
public class ConversationParticipant {

    @Id
    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Highest message id this user has read; null means nothing read yet. */
    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    /** Number of messages from the other participant not yet read. Kept in sync on send/read. */
    @Column(name = "unread_count", nullable = false)
    private long unreadCount = 0;

    public ConversationParticipant(Long conversationId, Long userId) {
        this.conversationId = conversationId;
        this.userId = userId;
    }
}
