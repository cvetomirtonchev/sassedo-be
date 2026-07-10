package server.sassedo.messaging.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.sassedo.messaging.data.dto.ConversationParticipant;
import server.sassedo.messaging.data.dto.ConversationParticipantId;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationParticipantRepository
        extends JpaRepository<ConversationParticipant, ConversationParticipantId> {

    Optional<ConversationParticipant> findByConversationIdAndUserId(Long conversationId, Long userId);

    List<ConversationParticipant> findByConversationIdInAndUserId(Collection<Long> conversationIds, Long userId);

    /** Global unread badge: number of conversations the user has at least one unread message in. */
    long countByUserIdAndUnreadCountGreaterThan(Long userId, long threshold);
}
