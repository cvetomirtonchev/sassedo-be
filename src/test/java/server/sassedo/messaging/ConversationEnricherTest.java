package server.sassedo.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.sassedo.messaging.data.dto.Conversation;
import server.sassedo.messaging.data.dto.ConversationParticipant;
import server.sassedo.messaging.data.dto.ConversationSummary;
import server.sassedo.messaging.repository.ConversationParticipantRepository;
import server.sassedo.messaging.service.ConversationEnricher;
import server.sassedo.user.data.projection.UserParticipantSummary;
import server.sassedo.user.repository.UserRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationEnricherTest {

    @Mock
    private ConversationParticipantRepository participantRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ConversationEnricher enricher;

    private Conversation conversation(Long id, Long p1, Long p2, String preview) {
        Conversation c = new Conversation();
        c.setId(id);
        c.setParticipant1Id(p1);
        c.setParticipant2Id(p2);
        c.setLastMessagePreview(preview);
        return c;
    }

    private UserParticipantSummary summary(Long id, String name, boolean hasPhoto) {
        UserParticipantSummary s = org.mockito.Mockito.mock(UserParticipantSummary.class);
        when(s.getId()).thenReturn(id);
        when(s.getName()).thenReturn(name);
        when(s.getHasPhoto()).thenReturn(hasPhoto);
        return s;
    }

    @Test
    void emptyInputSkipsAllQueries() {
        List<ConversationSummary> result = enricher.enrich(List.of(), 1L);

        assertThat(result).isEmpty();
        verifyNoInteractions(participantRepository, userRepository);
    }

    @Test
    void enrichesUnreadCountAndOtherParticipant() {
        Conversation c = conversation(10L, 1L, 2L, "hey there");
        UserParticipantSummary bob = summary(2L, "Bob", true);
        when(participantRepository.findByConversationIdInAndUserId(List.of(10L), 1L))
                .thenReturn(List.of(participant(10L, 1L, 3L)));
        when(userRepository.findParticipantSummariesByIdIn(anyCollection()))
                .thenReturn(List.of(bob));

        List<ConversationSummary> result = enricher.enrich(List.of(c), 1L);

        assertThat(result).hasSize(1);
        ConversationSummary s = result.get(0);
        assertThat(s.otherParticipantId()).isEqualTo(2L);
        assertThat(s.otherParticipantName()).isEqualTo("Bob");
        assertThat(s.otherParticipantHasPhoto()).isTrue();
        assertThat(s.lastMessagePreview()).isEqualTo("hey there");
        assertThat(s.unreadCount()).isEqualTo(3L);
    }

    @Test
    void defaultsUnreadToZeroWhenNoParticipantRow() {
        Conversation c = conversation(10L, 1L, 2L, null);
        when(participantRepository.findByConversationIdInAndUserId(any(), any())).thenReturn(List.of());
        when(userRepository.findParticipantSummariesByIdIn(anyCollection())).thenReturn(List.of());

        List<ConversationSummary> result = enricher.enrich(List.of(c), 1L);

        assertThat(result.get(0).unreadCount()).isZero();
        assertThat(result.get(0).otherParticipantName()).isNull();
        assertThat(result.get(0).otherParticipantHasPhoto()).isFalse();
    }

    private ConversationParticipant participant(Long conversationId, Long userId, long unread) {
        ConversationParticipant p = new ConversationParticipant(conversationId, userId);
        p.setUnreadCount(unread);
        return p;
    }
}
