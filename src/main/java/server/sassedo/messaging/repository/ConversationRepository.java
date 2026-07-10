package server.sassedo.messaging.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.sassedo.messaging.data.dto.Conversation;
import server.sassedo.promotion.common.ListingType;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findByParticipant1IdOrParticipant2IdOrderByLastMessageAtDesc(Long participant1Id,
                                                                                    Long participant2Id);

    Optional<Conversation> findByListingTypeAndListingIdAndParticipant1IdAndParticipant2Id(ListingType listingType,
                                                                                           Long listingId,
                                                                                           Long participant1Id,
                                                                                           Long participant2Id);

    /** Conversations that have no participant-state rows yet; drives the one-time backfill. */
    @Query("select c from Conversation c where c.id not in " +
            "(select distinct p.conversationId from ConversationParticipant p)")
    List<Conversation> findConversationsWithoutParticipants();

    /** Existing conversation(s) for a listing where the user is a participant. */
    @Query("select c from Conversation c where c.listingType = :listingType and c.listingId = :listingId " +
            "and (c.participant1Id = :userId or c.participant2Id = :userId) order by c.id asc")
    List<Conversation> findForListingAndUser(@Param("listingType") ListingType listingType,
                                             @Param("listingId") Long listingId,
                                             @Param("userId") Long userId);
}
