package server.sassedo.messaging.data.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for {@link ConversationParticipant}: (conversationId, userId).
 */
public class ConversationParticipantId implements Serializable {

    private Long conversationId;
    private Long userId;

    public ConversationParticipantId() {
    }

    public ConversationParticipantId(Long conversationId, Long userId) {
        this.conversationId = conversationId;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConversationParticipantId that)) {
            return false;
        }
        return Objects.equals(conversationId, that.conversationId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conversationId, userId);
    }
}
