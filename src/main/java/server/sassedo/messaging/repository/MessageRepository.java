package server.sassedo.messaging.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.sassedo.messaging.data.dto.Message;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    Optional<Message> findFirstByConversationIdOrderByCreatedAtDesc(Long conversationId);

    long countByConversationIdAndSenderIdNotAndIsReadFalse(Long conversationId, Long senderId);

    /** Idempotency lookup for retried sends. */
    Optional<Message> findBySenderIdAndClientMessageId(Long senderId, String clientMessageId);

    /**
     * Cursor page ordered newest-first. When {@code beforeId} is null the latest page is returned;
     * otherwise messages strictly older than the cursor. Callers reverse to ascending for display.
     */
    @Query("select m from Message m " +
            "where m.conversationId = :conversationId and (:beforeId is null or m.id < :beforeId) " +
            "order by m.id desc")
    List<Message> findPage(@Param("conversationId") Long conversationId,
                           @Param("beforeId") Long beforeId,
                           Pageable pageable);

    /** Unread messages from the other participant with id greater than the read cursor. */
    @Query("select count(m) from Message m " +
            "where m.conversationId = :conversationId and m.senderId <> :userId " +
            "and (:afterId is null or m.id > :afterId)")
    long countUnreadAfter(@Param("conversationId") Long conversationId,
                          @Param("userId") Long userId,
                          @Param("afterId") Long afterId);

    @Modifying
    @Query("update Message m set m.isRead = true " +
            "where m.conversationId = :conversationId and m.senderId <> :userId and m.isRead = false " +
            "and (:upToMessageId is null or m.id <= :upToMessageId)")
    int markConversationReadUpTo(@Param("conversationId") Long conversationId,
                                 @Param("userId") Long userId,
                                 @Param("upToMessageId") Long upToMessageId);

    @Query("select count(distinct m.conversationId) from Message m " +
            "where m.isRead = false and m.senderId <> :userId and m.conversationId in " +
            "(select c.id from Conversation c where c.participant1Id = :userId or c.participant2Id = :userId)")
    long countUnreadConversations(@Param("userId") Long userId);
}
