package server.sassedo.messaging.repository;

import org.springframework.data.jpa.repository.JpaRepository;
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
}
