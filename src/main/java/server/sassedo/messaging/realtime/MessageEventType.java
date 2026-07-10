package server.sassedo.messaging.realtime;

/**
 * Kinds of realtime events pushed to a client over SSE.
 */
public enum MessageEventType {
    /** A new message was created in a conversation the recipient participates in. */
    MESSAGE_CREATED,
    /** A conversation was marked read (typically by another tab of the same user). */
    CONVERSATION_READ,
    /** The user's global unread-conversation badge changed. */
    UNREAD_UPDATED
}
