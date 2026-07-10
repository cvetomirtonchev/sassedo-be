package server.sassedo.messaging.service;

import server.sassedo.messaging.data.dto.Conversation;
import server.sassedo.messaging.data.dto.ConversationSummary;
import server.sassedo.messaging.data.dto.Message;
import server.sassedo.model.GenericException;
import server.sassedo.promotion.common.ListingType;

import java.util.List;
import java.util.Optional;

public interface ConversationService {

    Conversation startOrGet(Long currentUserId, ListingType listingType, Long listingId) throws GenericException;

    /** Batch-enriched inbox rows for the header/popover; cost is independent of conversation count. */
    List<ConversationSummary> getUserConversationSummaries(Long userId);

    /** Enriches a single conversation (used after start/send to build the response). */
    ConversationSummary summarize(Conversation conversation, Long userId);

    /** Existing conversation for a listing the user participates in, if any (lazy lookup for the FE). */
    Optional<Conversation> findExistingForListing(Long userId, ListingType listingType, Long listingId);

    Conversation getById(Long conversationId, Long userId) throws GenericException;

    /**
     * Cursor page of messages ordered ascending. {@code beforeId} null returns the newest page; otherwise
     * messages strictly older than the cursor. {@code limit} is capped server-side.
     */
    List<Message> getMessages(Long conversationId, Long userId, Long beforeId, int limit) throws GenericException;

    Message sendMessage(Long conversationId, Long userId, String body, String clientMessageId) throws GenericException;

    void markRead(Long conversationId, Long userId, Long upToMessageId) throws GenericException;

    long getUnreadConversationCount(Long userId);
}
