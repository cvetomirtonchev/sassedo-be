package server.sassedo.messaging.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.sassedo.listing.rental.repository.RentalListingRepository;
import server.sassedo.listing.roommate.repository.RoommateListingRepository;
import server.sassedo.listing.search.repository.ApartmentSearchRepository;
import server.sassedo.messaging.data.dto.Conversation;
import server.sassedo.messaging.data.dto.ConversationParticipant;
import server.sassedo.messaging.data.dto.ConversationSummary;
import server.sassedo.messaging.data.dto.Message;
import server.sassedo.messaging.realtime.event.ConversationReadDomainEvent;
import server.sassedo.messaging.realtime.event.MessageSentDomainEvent;
import server.sassedo.messaging.repository.ConversationParticipantRepository;
import server.sassedo.messaging.repository.ConversationRepository;
import server.sassedo.messaging.repository.MessageRepository;
import server.sassedo.model.GenericException;
import server.sassedo.model.GenericExceptionCode;
import server.sassedo.promotion.common.ListingType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int PREVIEW_MAX_LENGTH = 500;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ConversationParticipantRepository participantRepository;
    private final ConversationEnricher conversationEnricher;
    private final ApplicationEventPublisher eventPublisher;
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
                        Conversation saved = conversationRepository.saveAndFlush(conversation);
                        ensureParticipantRow(saved.getId(), p1);
                        ensureParticipantRow(saved.getId(), p2);
                        return saved;
                    } catch (DataIntegrityViolationException e) {
                        // Lost a race with a concurrent create; re-fetch the existing conversation.
                        return conversationRepository
                                .findByListingTypeAndListingIdAndParticipant1IdAndParticipant2Id(listingType, listingId, p1, p2)
                                .orElseThrow(() -> e);
                    }
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationSummary> getUserConversationSummaries(Long userId) {
        List<Conversation> conversations =
                conversationRepository.findByParticipant1IdOrParticipant2IdOrderByLastMessageAtDesc(userId, userId);
        return conversationEnricher.enrich(conversations, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationSummary summarize(Conversation conversation, Long userId) {
        List<ConversationSummary> summaries = conversationEnricher.enrich(List.of(conversation), userId);
        return summaries.isEmpty() ? null : summaries.get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Conversation> findExistingForListing(Long userId, ListingType listingType, Long listingId) {
        List<Conversation> matches = conversationRepository.findForListingAndUser(listingType, listingId, userId);
        return matches.isEmpty() ? Optional.empty() : Optional.of(matches.get(0));
    }

    @Override
    public Conversation getById(Long conversationId, Long userId) throws GenericException {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new GenericException(GenericExceptionCode.CONVERSATION_NOT_FOUND, "Conversation not found"));
        ensureParticipant(conversation, userId);
        return conversation;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getMessages(Long conversationId, Long userId, Long beforeId, int limit) throws GenericException {
        getById(conversationId, userId);
        int capped = Math.min(Math.max(limit, 1), MAX_PAGE_SIZE);
        List<Message> descending =
                messageRepository.findPage(conversationId, beforeId, PageRequest.of(0, capped));
        List<Message> ascending = new ArrayList<>(descending);
        Collections.reverse(ascending);
        return ascending;
    }

    @Override
    @Transactional
    public Message sendMessage(Long conversationId, Long userId, String body, String clientMessageId)
            throws GenericException {
        Conversation conversation = getById(conversationId, userId);

        if (clientMessageId != null && !clientMessageId.isBlank()) {
            Optional<Message> existing = messageRepository.findBySenderIdAndClientMessageId(userId, clientMessageId);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        Long recipientId = otherParticipant(conversation, userId);

        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderId(userId);
        message.setClientMessageId(clientMessageId);
        message.setMessage(body);
        message.setRead(false);
        Message saved = messageRepository.save(message);

        conversation.setLastMessageAt(saved.getCreatedAt());
        conversation.setLastMessageId(saved.getId());
        conversation.setLastMessagePreview(preview(body));
        conversationRepository.save(conversation);

        ConversationParticipant recipientState = ensureParticipantRow(conversationId, recipientId);
        recipientState.setUnreadCount(recipientState.getUnreadCount() + 1);
        participantRepository.save(recipientState);

        eventPublisher.publishEvent(new MessageSentDomainEvent(conversationId, saved.getId(), userId, recipientId,
                saved.getClientMessageId(), saved.getMessage(), saved.getCreatedAt()));

        return saved;
    }

    @Override
    @Transactional
    public void markRead(Long conversationId, Long userId, Long upToMessageId) throws GenericException {
        Conversation conversation = getById(conversationId, userId);

        messageRepository.markConversationReadUpTo(conversationId, userId, upToMessageId);

        ConversationParticipant state = ensureParticipantRow(conversationId, userId);
        Long newCursor = upToMessageId != null ? upToMessageId : conversation.getLastMessageId();
        if (newCursor != null
                && (state.getLastReadMessageId() == null || newCursor > state.getLastReadMessageId())) {
            state.setLastReadMessageId(newCursor);
        }
        long remaining = messageRepository.countUnreadAfter(conversationId, userId, state.getLastReadMessageId());
        state.setUnreadCount(remaining);
        participantRepository.save(state);

        eventPublisher.publishEvent(new ConversationReadDomainEvent(conversationId, userId));
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadConversationCount(Long userId) {
        return participantRepository.countByUserIdAndUnreadCountGreaterThan(userId, 0L);
    }

    private ConversationParticipant ensureParticipantRow(Long conversationId, Long userId) {
        return participantRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseGet(() -> participantRepository.save(new ConversationParticipant(conversationId, userId)));
    }

    private Long otherParticipant(Conversation conversation, Long userId) {
        return userId.equals(conversation.getParticipant1Id())
                ? conversation.getParticipant2Id()
                : conversation.getParticipant1Id();
    }

    private String preview(String body) {
        if (body == null) {
            return null;
        }
        String trimmed = body.strip();
        return trimmed.length() <= PREVIEW_MAX_LENGTH ? trimmed : trimmed.substring(0, PREVIEW_MAX_LENGTH);
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
