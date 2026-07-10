package server.sassedo.messaging.realtime;

import lombok.Getter;
import server.sassedo.messaging.data.dto.Message;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Serialized payload delivered to a subscribed client. Carries enough data for the frontend to update
 * its React Query caches directly (append the message, re-sort the inbox, set the badge) without an
 * extra fetch. Fields not relevant to a given {@link MessageEventType} are left null.
 */
@Getter
public class MessageEvent {

    private final String eventId = UUID.randomUUID().toString();
    private final MessageEventType type;
    private final Long conversationId;
    private final Long messageId;
    private final Long senderId;
    private final String clientMessageId;
    private final String message;
    private final LocalDateTime createdAt;
    private final Long totalUnreadConversations;

    private MessageEvent(MessageEventType type, Long conversationId, Long messageId, Long senderId,
                         String clientMessageId, String message, LocalDateTime createdAt,
                         Long totalUnreadConversations) {
        this.type = type;
        this.conversationId = conversationId;
        this.messageId = messageId;
        this.senderId = senderId;
        this.clientMessageId = clientMessageId;
        this.message = message;
        this.createdAt = createdAt;
        this.totalUnreadConversations = totalUnreadConversations;
    }

    public static MessageEvent messageCreated(Message message, Long totalUnreadConversations) {
        return new MessageEvent(MessageEventType.MESSAGE_CREATED, message.getConversationId(), message.getId(),
                message.getSenderId(), message.getClientMessageId(), message.getMessage(), message.getCreatedAt(),
                totalUnreadConversations);
    }

    public static MessageEvent conversationRead(Long conversationId, Long totalUnreadConversations) {
        return new MessageEvent(MessageEventType.CONVERSATION_READ, conversationId, null, null, null, null, null,
                totalUnreadConversations);
    }

    public static MessageEvent unreadUpdated(Long totalUnreadConversations) {
        return new MessageEvent(MessageEventType.UNREAD_UPDATED, null, null, null, null, null, null,
                totalUnreadConversations);
    }

    /** Lower-cased SSE {@code event:} name derived from the type. */
    public String eventName() {
        return type.name().toLowerCase();
    }
}
