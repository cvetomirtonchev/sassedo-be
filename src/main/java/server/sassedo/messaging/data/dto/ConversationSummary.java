package server.sassedo.messaging.data.dto;

/**
 * Fully-enriched inbox row assembled with a fixed number of batch queries (no per-row lookups).
 * Carries everything the controller needs to build a {@code ConversationResponse}.
 */
public record ConversationSummary(
        Conversation conversation,
        Long otherParticipantId,
        String otherParticipantName,
        boolean otherParticipantHasPhoto,
        String lastMessagePreview,
        long unreadCount) {
}
