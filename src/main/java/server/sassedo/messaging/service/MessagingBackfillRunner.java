package server.sassedo.messaging.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import server.sassedo.messaging.data.dto.Conversation;
import server.sassedo.messaging.data.dto.ConversationParticipant;
import server.sassedo.messaging.data.dto.Message;
import server.sassedo.messaging.repository.ConversationParticipantRepository;
import server.sassedo.messaging.repository.ConversationRepository;
import server.sassedo.messaging.repository.MessageRepository;

import java.util.List;

/**
 * One-time, idempotent migration of legacy conversations to the participant-state model. On startup it
 * finds conversations that have no {@link ConversationParticipant} rows, seeds both rows with unread
 * counts derived from the existing {@code is_read} flags, and backfills the denormalized
 * {@code lastMessageId}/{@code lastMessagePreview}. Subsequent startups find nothing to do.
 */
@Component
@Order(50)
@RequiredArgsConstructor
public class MessagingBackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MessagingBackfillRunner.class);
    private static final int PREVIEW_MAX_LENGTH = 500;

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Conversation> pending = conversationRepository.findConversationsWithoutParticipants();
        if (pending.isEmpty()) {
            return;
        }
        log.info("Messaging backfill: seeding participant state for {} conversation(s)", pending.size());

        for (Conversation conversation : pending) {
            backfillConversation(conversation);
        }
        conversationRepository.saveAll(pending);
        log.info("Messaging backfill: completed for {} conversation(s)", pending.size());
    }

    private void backfillConversation(Conversation conversation) {
        Long conversationId = conversation.getId();

        seedParticipant(conversationId, conversation.getParticipant1Id());
        seedParticipant(conversationId, conversation.getParticipant2Id());

        if (conversation.getLastMessageId() == null || conversation.getLastMessagePreview() == null) {
            messageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversationId)
                    .ifPresent(last -> {
                        conversation.setLastMessageId(last.getId());
                        conversation.setLastMessagePreview(preview(last.getMessage()));
                        if (conversation.getLastMessageAt() == null) {
                            conversation.setLastMessageAt(last.getCreatedAt());
                        }
                    });
        }
    }

    private void seedParticipant(Long conversationId, Long userId) {
        if (userId == null || participantRepository.findByConversationIdAndUserId(conversationId, userId).isPresent()) {
            return;
        }
        ConversationParticipant participant = new ConversationParticipant(conversationId, userId);
        long unread = messageRepository.countByConversationIdAndSenderIdNotAndIsReadFalse(conversationId, userId);
        participant.setUnreadCount(unread);
        participantRepository.save(participant);
    }

    private String preview(String body) {
        if (body == null) {
            return null;
        }
        String trimmed = body.strip();
        return trimmed.length() <= PREVIEW_MAX_LENGTH ? trimmed : trimmed.substring(0, PREVIEW_MAX_LENGTH);
    }
}
