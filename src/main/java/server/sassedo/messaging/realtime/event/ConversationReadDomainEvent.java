package server.sassedo.messaging.realtime.event;

/**
 * Published inside the mark-read transaction; delivered after commit so the user's other tabs/devices
 * update their unread badge.
 */
public record ConversationReadDomainEvent(Long conversationId, Long userId) {
}
