package server.sassedo.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import server.sassedo.listing.rental.data.dto.RentalListing;
import server.sassedo.listing.rental.repository.RentalListingRepository;
import server.sassedo.listing.roommate.repository.RoommateListingRepository;
import server.sassedo.messaging.data.dto.Conversation;
import server.sassedo.messaging.data.dto.ConversationParticipant;
import server.sassedo.messaging.data.dto.Message;
import server.sassedo.messaging.realtime.event.ConversationReadDomainEvent;
import server.sassedo.messaging.realtime.event.MessageSentDomainEvent;
import server.sassedo.messaging.repository.ConversationParticipantRepository;
import server.sassedo.messaging.repository.ConversationRepository;
import server.sassedo.messaging.repository.MessageRepository;
import server.sassedo.messaging.service.ConversationEnricher;
import server.sassedo.messaging.service.ConversationServiceImpl;
import server.sassedo.model.GenericException;
import server.sassedo.promotion.common.ListingType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationServiceImplTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private ConversationParticipantRepository participantRepository;
    @Mock
    private ConversationEnricher conversationEnricher;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private RentalListingRepository rentalRepository;
    @Mock
    private RoommateListingRepository roommateRepository;

    @InjectMocks
    private ConversationServiceImpl service;

    private Conversation conversation(Long id, Long p1, Long p2) {
        Conversation c = new Conversation();
        c.setId(id);
        c.setParticipant1Id(p1);
        c.setParticipant2Id(p2);
        return c;
    }

    private Message message(Long id, Long conversationId, Long senderId) {
        Message m = new Message();
        m.setId(id);
        m.setConversationId(conversationId);
        m.setSenderId(senderId);
        m.setMessage("body");
        return m;
    }

    @Test
    void sendMessageReturnsExistingOnDuplicateClientId() throws GenericException {
        Conversation c = conversation(10L, 1L, 2L);
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(c));
        Message existing = message(99L, 10L, 1L);
        existing.setClientMessageId("abc");
        when(messageRepository.findBySenderIdAndClientMessageId(1L, "abc")).thenReturn(Optional.of(existing));

        Message result = service.sendMessage(10L, 1L, "hi again", "abc");

        assertThat(result).isSameAs(existing);
        verify(messageRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void sendMessagePersistsIncrementsRecipientUnreadAndPublishes() throws GenericException {
        Conversation c = conversation(10L, 1L, 2L);
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(c));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message m = invocation.getArgument(0);
            m.setId(101L);
            m.setCreatedAt(LocalDateTime.now());
            return m;
        });
        when(participantRepository.findByConversationIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(new ConversationParticipant(10L, 2L)));

        Message saved = service.sendMessage(10L, 1L, "hello", null);

        assertThat(saved.getId()).isEqualTo(101L);
        assertThat(c.getLastMessageId()).isEqualTo(101L);
        assertThat(c.getLastMessagePreview()).isEqualTo("hello");

        ArgumentCaptor<ConversationParticipant> captor = ArgumentCaptor.forClass(ConversationParticipant.class);
        verify(participantRepository).save(captor.capture());
        assertThat(captor.getValue().getUnreadCount()).isEqualTo(1L);

        ArgumentCaptor<MessageSentDomainEvent> eventCaptor = ArgumentCaptor.forClass(MessageSentDomainEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().recipientId()).isEqualTo(2L);
        assertThat(eventCaptor.getValue().senderId()).isEqualTo(1L);
    }

    @Test
    void markReadRecomputesUnreadAndAdvancesCursor() throws GenericException {
        Conversation c = conversation(10L, 1L, 2L);
        c.setLastMessageId(50L);
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(c));
        when(participantRepository.findByConversationIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(new ConversationParticipant(10L, 1L)));
        when(messageRepository.countUnreadAfter(eq(10L), eq(1L), any())).thenReturn(0L);

        service.markRead(10L, 1L, null);

        verify(messageRepository).markConversationReadUpTo(10L, 1L, null);
        ArgumentCaptor<ConversationParticipant> captor = ArgumentCaptor.forClass(ConversationParticipant.class);
        verify(participantRepository).save(captor.capture());
        assertThat(captor.getValue().getLastReadMessageId()).isEqualTo(50L);
        assertThat(captor.getValue().getUnreadCount()).isEqualTo(0L);
        verify(eventPublisher).publishEvent(any(ConversationReadDomainEvent.class));
    }

    @Test
    void getMessagesCapsLimitAndReturnsAscending() throws GenericException {
        Conversation c = conversation(10L, 1L, 2L);
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(c));
        List<Message> descending = new ArrayList<>(List.of(
                message(3L, 10L, 2L),
                message(2L, 10L, 1L),
                message(1L, 10L, 2L)));
        when(messageRepository.findPage(eq(10L), isNull(), any(PageRequest.class))).thenReturn(descending);

        List<Message> ascending = service.getMessages(10L, 1L, null, 999);

        assertThat(ascending).extracting(Message::getId).containsExactly(1L, 2L, 3L);
    }

    @Test
    void getUnreadConversationCountDelegatesToParticipantRepository() {
        when(participantRepository.countByUserIdAndUnreadCountGreaterThan(1L, 0L)).thenReturn(4L);
        assertThat(service.getUnreadConversationCount(1L)).isEqualTo(4L);
    }

    @Test
    void startOrGetRejectsMessagingYourself() {
        RentalListing listing = org.mockito.Mockito.mock(RentalListing.class);
        when(listing.getOwnerId()).thenReturn(1L);
        when(rentalRepository.findById(5L)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> service.startOrGet(1L, ListingType.RENTAL, 5L))
                .isInstanceOf(GenericException.class);
    }
}
