package server.sassedo.messaging.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import server.sassedo.messaging.data.dto.Message;
import server.sassedo.messaging.realtime.event.ConversationReadDomainEvent;
import server.sassedo.messaging.realtime.event.MessageSentDomainEvent;
import server.sassedo.messaging.repository.ConversationParticipantRepository;

/**
 * Bridges committed domain changes to realtime delivery. Runs only after the originating transaction
 * commits (AFTER_COMMIT), guaranteeing that any recipient reacting to an event sees persisted state.
 */
@Component
@RequiredArgsConstructor
public class MessagingRealtimeNotifier {

    private final MessageRealtimeGateway gateway;
    private final ConversationParticipantRepository participantRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageSent(MessageSentDomainEvent event) {
        Message message = toMessage(event);

        // Recipient: new message plus refreshed badge.
        long recipientUnread = unreadConversations(event.recipientId());
        gateway.publishToUser(event.recipientId(), MessageEvent.messageCreated(message, recipientUnread));

        // Sender's other tabs/devices: mirror the message so their open thread and inbox stay in sync.
        long senderUnread = unreadConversations(event.senderId());
        gateway.publishToUser(event.senderId(), MessageEvent.messageCreated(message, senderUnread));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onConversationRead(ConversationReadDomainEvent event) {
        long unread = unreadConversations(event.userId());
        gateway.publishToUser(event.userId(), MessageEvent.conversationRead(event.conversationId(), unread));
    }

    private long unreadConversations(Long userId) {
        return participantRepository.countByUserIdAndUnreadCountGreaterThan(userId, 0L);
    }

    private Message toMessage(MessageSentDomainEvent event) {
        Message message = new Message();
        message.setId(event.messageId());
        message.setConversationId(event.conversationId());
        message.setSenderId(event.senderId());
        message.setClientMessageId(event.clientMessageId());
        message.setMessage(event.message());
        message.setCreatedAt(event.createdAt());
        message.setRead(false);
        return message;
    }
}
