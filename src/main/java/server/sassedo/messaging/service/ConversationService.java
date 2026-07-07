package server.sassedo.messaging.service;

import server.sassedo.messaging.data.dto.Conversation;
import server.sassedo.messaging.data.dto.Message;
import server.sassedo.model.GenericException;
import server.sassedo.promotion.common.ListingType;

import java.util.List;

public interface ConversationService {

    Conversation startOrGet(Long currentUserId, ListingType listingType, Long listingId) throws GenericException;

    List<Conversation> getUserConversations(Long userId);

    Conversation getById(Long conversationId, Long userId) throws GenericException;

    List<Message> getMessages(Long conversationId, Long userId) throws GenericException;

    Message sendMessage(Long conversationId, Long userId, String body) throws GenericException;

    void markRead(Long conversationId, Long userId) throws GenericException;

    long getUnreadConversationCount(Long userId);

    Message getLastMessage(Long conversationId);

    long getUnreadCount(Long conversationId, Long userId);
}
