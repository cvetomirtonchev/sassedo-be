package server.sassedo.messaging.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.sassedo.listing.rental.repository.RentalListingRepository;
import server.sassedo.listing.roommate.repository.RoommateListingRepository;
import server.sassedo.listing.search.repository.ApartmentSearchRepository;
import server.sassedo.messaging.data.dto.Conversation;
import server.sassedo.messaging.data.dto.Message;
import server.sassedo.messaging.repository.ConversationRepository;
import server.sassedo.messaging.repository.MessageRepository;
import server.sassedo.model.GenericException;
import server.sassedo.model.GenericExceptionCode;
import server.sassedo.promotion.common.ListingType;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final RentalListingRepository rentalRepository;
    private final RoommateListingRepository roommateRepository;
    private final ApartmentSearchRepository searchRepository;

    @Override
    @Transactional
    public Conversation startOrGet(Long currentUserId, ListingType listingType, Long listingId) throws GenericException {
        ListingContext context = resolveListing(listingType, listingId);
        Long ownerId = context.ownerId();

        if (ownerId == null) {
            throw new GenericException(GenericExceptionCode.LISTING_NOT_FOUND, "Listing not found");
        }
        if (ownerId.equals(currentUserId)) {
            throw new GenericException(GenericExceptionCode.CANNOT_MESSAGE_SELF, "You cannot message yourself");
        }

        Long p1 = Math.min(currentUserId, ownerId);
        Long p2 = Math.max(currentUserId, ownerId);

        return conversationRepository
                .findByListingTypeAndListingIdAndParticipant1IdAndParticipant2Id(listingType, listingId, p1, p2)
                .orElseGet(() -> {
                    Conversation conversation = new Conversation();
                    conversation.setListingType(listingType);
                    conversation.setListingId(listingId);
                    conversation.setParticipant1Id(p1);
                    conversation.setParticipant2Id(p2);
                    conversation.setTitle(context.title());
                    try {
                        return conversationRepository.saveAndFlush(conversation);
                    } catch (DataIntegrityViolationException e) {
                        // Lost a race with a concurrent create; re-fetch the existing conversation.
                        return conversationRepository
                                .findByListingTypeAndListingIdAndParticipant1IdAndParticipant2Id(listingType, listingId, p1, p2)
                                .orElseThrow(() -> e);
                    }
                });
    }

    @Override
    public List<Conversation> getUserConversations(Long userId) {
        return conversationRepository.findByParticipant1IdOrParticipant2IdOrderByLastMessageAtDesc(userId, userId);
    }

    @Override
    public Conversation getById(Long conversationId, Long userId) throws GenericException {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new GenericException(GenericExceptionCode.CONVERSATION_NOT_FOUND, "Conversation not found"));
        ensureParticipant(conversation, userId);
        return conversation;
    }

    @Override
    public List<Message> getMessages(Long conversationId, Long userId) throws GenericException {
        getById(conversationId, userId);
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    @Override
    @Transactional
    public Message sendMessage(Long conversationId, Long userId, String body) throws GenericException {
        Conversation conversation = getById(conversationId, userId);

        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderId(userId);
        message.setMessage(body);
        message.setRead(false);
        Message saved = messageRepository.save(message);

        conversation.setLastMessageAt(saved.getCreatedAt());
        conversationRepository.save(conversation);

        return saved;
    }

    @Override
    @Transactional
    public void markRead(Long conversationId, Long userId) throws GenericException {
        getById(conversationId, userId);
        messageRepository.markConversationRead(conversationId, userId);
    }

    @Override
    public long getUnreadConversationCount(Long userId) {
        return messageRepository.countUnreadConversations(userId);
    }

    @Override
    public Message getLastMessage(Long conversationId) {
        return messageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversationId).orElse(null);
    }

    @Override
    public long getUnreadCount(Long conversationId, Long userId) {
        return messageRepository.countByConversationIdAndSenderIdNotAndIsReadFalse(conversationId, userId);
    }

    private void ensureParticipant(Conversation conversation, Long userId) throws GenericException {
        if (userId == null
                || (!userId.equals(conversation.getParticipant1Id()) && !userId.equals(conversation.getParticipant2Id()))) {
            throw new GenericException(GenericExceptionCode.NOT_CONVERSATION_PARTICIPANT,
                    "You are not a participant of this conversation");
        }
    }

    /**
     * Resolves the owner id and denormalized title of a polymorphic post, mirroring
     * {@code PromotableListingServiceImpl.getOwnerId} across the three listing tables.
     */
    private ListingContext resolveListing(ListingType type, Long listingId) throws GenericException {
        return switch (type) {
            case RENTAL -> rentalRepository.findById(listingId)
                    .map(l -> new ListingContext(l.getOwnerId(), l.getTitle()))
                    .orElseThrow(this::listingNotFound);
            case ROOMMATE -> roommateRepository.findById(listingId)
                    .map(l -> new ListingContext(l.getOwnerId(), l.getTitle()))
                    .orElseThrow(this::listingNotFound);
            case SEARCH -> searchRepository.findById(listingId)
                    .map(l -> new ListingContext(l.getOwnerId(), l.getTitle()))
                    .orElseThrow(this::listingNotFound);
        };
    }

    private GenericException listingNotFound() {
        return new GenericException(GenericExceptionCode.LISTING_NOT_FOUND, "Listing not found");
    }

    private record ListingContext(Long ownerId, String title) {
    }
}
