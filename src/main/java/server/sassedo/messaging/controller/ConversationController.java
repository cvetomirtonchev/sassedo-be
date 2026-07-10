package server.sassedo.messaging.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import server.sassedo.messaging.data.dto.Conversation;
import server.sassedo.messaging.data.dto.ConversationSummary;
import server.sassedo.messaging.data.dto.Message;
import server.sassedo.messaging.data.network.request.MarkReadRequest;
import server.sassedo.messaging.data.network.request.SendMessageRequest;
import server.sassedo.messaging.data.network.request.StartConversationRequest;
import server.sassedo.messaging.data.network.response.ConversationResponse;
import server.sassedo.messaging.data.network.response.MessageResponse;
import server.sassedo.messaging.service.ConversationService;
import server.sassedo.model.GenericException;
import server.sassedo.promotion.common.ListingType;
import server.sassedo.security.jwt.JwtUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static server.sassedo.utils.ServerUtils.getUserId;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private static final int DEFAULT_MESSAGE_PAGE_SIZE = 50;

    private final ConversationService conversationService;
    private final JwtUtils jwtUtils;

    @PostMapping
    public ResponseEntity<?> start(@RequestBody StartConversationRequest request, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            Conversation conversation = conversationService.startOrGet(userId, request.getListingType(), request.getListingId());
            return ResponseEntity.ok(mapToResponse(conversationService.summarize(conversation, userId)));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @GetMapping
    public ResponseEntity<?> getConversations(HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        List<ConversationResponse> conversations = conversationService.getUserConversationSummaries(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        return ResponseEntity.ok(Map.of("count", conversationService.getUnreadConversationCount(userId)));
    }

    @GetMapping("/lookup")
    public ResponseEntity<?> lookup(@RequestParam ListingType listingType,
                                    @RequestParam Long listingId,
                                    HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        Optional<Conversation> existing = conversationService.findExistingForListing(userId, listingType, listingId);
        Map<String, Object> body = new HashMap<>();
        body.put("conversationId", existing.map(Conversation::getId).orElse(null));
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<?> getMessages(@PathVariable Long id,
                                         @RequestParam(required = false) Long beforeId,
                                         @RequestParam(required = false) Integer limit,
                                         HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            int pageSize = limit != null ? limit : DEFAULT_MESSAGE_PAGE_SIZE;
            List<MessageResponse> messages = conversationService.getMessages(id, userId, beforeId, pageSize).stream()
                    .map(this::mapMessage)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(messages);
        } catch (GenericException e) {
            return ResponseEntity.status(404).body(e.getErrorResponse());
        }
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<?> sendMessage(@PathVariable Long id, @Valid @RequestBody SendMessageRequest request,
                                         HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            Message message = conversationService.sendMessage(id, userId, request.getMessage(),
                    request.getClientMessageId());
            return ResponseEntity.ok(mapMessage(message));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable Long id,
                                      @RequestBody(required = false) MarkReadRequest request,
                                      HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            Long upToMessageId = request != null ? request.getUpToMessageId() : null;
            conversationService.markRead(id, userId, upToMessageId);
            return ResponseEntity.ok().build();
        } catch (GenericException e) {
            return ResponseEntity.status(404).body(e.getErrorResponse());
        }
    }

    private ConversationResponse mapToResponse(ConversationSummary summary) {
        if (summary == null) {
            return null;
        }
        Conversation conversation = summary.conversation();
        ConversationResponse response = new ConversationResponse();
        response.setId(conversation.getId());
        response.setListingType(conversation.getListingType());
        response.setListingId(conversation.getListingId());
        response.setTitle(conversation.getTitle());
        response.setLastMessageAt(conversation.getLastMessageAt());
        response.setLastMessageId(conversation.getLastMessageId());
        response.setCreatedAt(conversation.getCreatedAt());
        response.setUpdatedAt(conversation.getUpdatedAt());

        response.setOtherParticipantId(summary.otherParticipantId());
        response.setOtherParticipantName(summary.otherParticipantName());
        if (summary.otherParticipantId() != null && summary.otherParticipantHasPhoto()) {
            response.setOtherParticipantPhotoUrl(buildPhotoUrl(summary.otherParticipantId()));
        }
        response.setLastMessagePreview(summary.lastMessagePreview());
        response.setUnreadCount(summary.unreadCount());
        return response;
    }

    private String buildPhotoUrl(Long userId) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/user/")
                .path(String.valueOf(userId))
                .path("/picture")
                .toUriString();
    }

    private MessageResponse mapMessage(Message message) {
        MessageResponse response = new MessageResponse();
        response.setId(message.getId());
        response.setConversationId(message.getConversationId());
        response.setSenderId(message.getSenderId());
        response.setClientMessageId(message.getClientMessageId());
        response.setMessage(message.getMessage());
        response.setCreatedAt(message.getCreatedAt());
        response.setRead(message.isRead());
        return response;
    }
}
