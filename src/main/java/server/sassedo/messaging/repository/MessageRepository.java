package server.sassedo.messaging.repository;

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

    @Modifying
    @Query("update Message m set m.isRead = true " +
            "where m.conversationId = :conversationId and m.senderId <> :userId and m.isRead = false")
    int markConversationRead(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    @Query("select count(distinct m.conversationId) from Message m " +
            "where m.isRead = false and m.senderId <> :userId and m.conversationId in " +
            "(select c.id from Conversation c where c.participant1Id = :userId or c.participant2Id = :userId)")
    long countUnreadConversations(@Param("userId") Long userId);
}
