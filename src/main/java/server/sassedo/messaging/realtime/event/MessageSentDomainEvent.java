package server.sassedo.messaging.realtime.event;

/**
 * Published inside the send transaction; delivered to SSE subscribers only after commit so a client
 * that reacts by re-fetching is guaranteed to see the persisted data.
 */
public record MessageSentDomainEvent(Long conversationId, Long messageId, Long senderId, Long recipientId,
                                     String clientMessageId, String message,
                                     java.time.LocalDateTime createdAt) {
}
