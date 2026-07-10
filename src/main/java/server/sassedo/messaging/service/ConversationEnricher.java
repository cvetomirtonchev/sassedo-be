package server.sassedo.messaging.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import server.sassedo.messaging.data.dto.Conversation;
import server.sassedo.messaging.data.dto.ConversationParticipant;
import server.sassedo.messaging.data.dto.ConversationSummary;
import server.sassedo.messaging.repository.ConversationParticipantRepository;
import server.sassedo.user.data.projection.UserParticipantSummary;
import server.sassedo.user.repository.UserRepository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Turns a list of {@link Conversation}s into fully-populated {@link ConversationSummary} rows using a
 * fixed number of batch queries, mirroring the pattern in {@code EngagementEnricher}. This replaces the
 * previous per-row (N+1) enrichment in the controller so inbox cost is independent of conversation count.
 */
@Component
@RequiredArgsConstructor
public class ConversationEnricher {

    private final ConversationParticipantRepository participantRepository;
    private final UserRepository userRepository;

    public List<ConversationSummary> enrich(List<Conversation> conversations, Long userId) {
        if (conversations == null || conversations.isEmpty()) {
            return List.of();
        }

        List<Long> conversationIds = conversations.stream().map(Conversation::getId).toList();

        Map<Long, Long> unreadByConversation = participantRepository
                .findByConversationIdInAndUserId(conversationIds, userId).stream()
                .collect(Collectors.toMap(ConversationParticipant::getConversationId,
                        ConversationParticipant::getUnreadCount));

        Set<Long> otherIds = conversations.stream()
                .map(c -> otherParticipant(c, userId))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, UserParticipantSummary> usersById = otherIds.isEmpty()
                ? Collections.emptyMap()
                : userRepository.findParticipantSummariesByIdIn(otherIds).stream()
                        .collect(Collectors.toMap(UserParticipantSummary::getId, Function.identity()));

        return conversations.stream().map(conversation -> {
            Long otherId = otherParticipant(conversation, userId);
            UserParticipantSummary other = otherId != null ? usersById.get(otherId) : null;
            return new ConversationSummary(
                    conversation,
                    otherId,
                    other != null ? other.getName() : null,
                    other != null && other.getHasPhoto(),
                    conversation.getLastMessagePreview(),
                    unreadByConversation.getOrDefault(conversation.getId(), 0L));
        }).toList();
    }

    private Long otherParticipant(Conversation conversation, Long userId) {
        if (userId == null) {
            return null;
        }
        return userId.equals(conversation.getParticipant1Id())
                ? conversation.getParticipant2Id()
                : conversation.getParticipant1Id();
    }
}
