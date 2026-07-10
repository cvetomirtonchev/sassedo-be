package server.sassedo.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.sassedo.messaging.realtime.MessageEvent;
import server.sassedo.messaging.realtime.MessageEventType;
import server.sassedo.messaging.realtime.MessageRealtimeGateway;
import server.sassedo.messaging.realtime.MessagingRealtimeNotifier;
import server.sassedo.messaging.realtime.event.ConversationReadDomainEvent;
import server.sassedo.messaging.realtime.event.MessageSentDomainEvent;
import server.sassedo.messaging.repository.ConversationParticipantRepository;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessagingRealtimeNotifierTest {

    @Mock
    private MessageRealtimeGateway gateway;
    @Mock
    private ConversationParticipantRepository participantRepository;

    @InjectMocks
    private MessagingRealtimeNotifier notifier;

    @Test
    void messageSentNotifiesRecipientAndSender() {
        when(participantRepository.countByUserIdAndUnreadCountGreaterThan(2L, 0L)).thenReturn(1L);
        when(participantRepository.countByUserIdAndUnreadCountGreaterThan(1L, 0L)).thenReturn(0L);

        notifier.onMessageSent(new MessageSentDomainEvent(10L, 101L, 1L, 2L, null, "hi", LocalDateTime.now()));

        verify(gateway).publishToUser(eq(2L), argThat(event ->
                event.getType() == MessageEventType.MESSAGE_CREATED
                        && event.getMessageId() == 101L
                        && event.getTotalUnreadConversations() == 1L));
        verify(gateway).publishToUser(eq(1L), argThat(event ->
                event.getType() == MessageEventType.MESSAGE_CREATED
                        && event.getTotalUnreadConversations() == 0L));
    }

    @Test
    void conversationReadNotifiesUser() {
        when(participantRepository.countByUserIdAndUnreadCountGreaterThan(5L, 0L)).thenReturn(2L);

        notifier.onConversationRead(new ConversationReadDomainEvent(10L, 5L));

        verify(gateway).publishToUser(eq(5L), argThat(event ->
                event.getType() == MessageEventType.CONVERSATION_READ
                        && event.getConversationId() == 10L
                        && event.getTotalUnreadConversations() == 2L));
    }

    @Test
    void unreadUpdatedFactoryProducesBadgeOnlyEvent() {
        MessageEvent event = MessageEvent.unreadUpdated(7L);
        assertThat(event.getType()).isEqualTo(MessageEventType.UNREAD_UPDATED);
        assertThat(event.getTotalUnreadConversations()).isEqualTo(7L);
        assertThat(event.getConversationId()).isNull();
    }
}
