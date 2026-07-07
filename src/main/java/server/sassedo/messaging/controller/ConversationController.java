package server.sassedo.messaging.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import server.sassedo.messaging.data.dto.Conversation;
import server.sassedo.messaging.data.dto.Message;
import server.sassedo.messaging.data.network.request.SendMessageRequest;
import server.sassedo.messaging.data.network.request.StartConversationRequest;
import server.sassedo.messaging.data.network.response.ConversationResponse;
import server.sassedo.messaging.data.network.response.MessageResponse;
import server.sassedo.messaging.service.ConversationService;
import server.sassedo.model.GenericException;
import server.sassedo.security.jwt.JwtUtils;
import server.sassedo.user.data.dto.User;
import server.sassedo.user.service.user.UserService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static server.sassedo.utils.ServerUtils.getUserId;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final UserService userService;
    private final JwtUtils jwtUtils;

    @PostMapping
    public ResponseEntity<?> start(@RequestBody StartConversationRequest request, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            Conversation conversation = conversationService.startOrGet(userId, request.getListingType(), request.getListingId());
            return ResponseEntity.ok(mapToResponse(conversation, userId));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @GetMapping
    public ResponseEntity<?> getConversations(HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        List<ConversationResponse> conversations = conversationService.getUserConversations(userId).stream()
                .map(c -> mapToResponse(c, userId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        return ResponseEntity.ok(Map.of("count", conversationService.getUnreadConversationCount(userId)));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<?> getMessages(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            List<MessageResponse> messages = conversationService.getMessages(id, userId).stream()
                    .map(this::mapMessage)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(messages);
        } catch (GenericException e) {
            return ResponseEntity.status(404).body(e.getErrorResponse());
        }
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<?> sendMessage(@PathVariable Long id, @RequestBody SendMessageRequest request,
                                         HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            Message message = conversationService.sendMessage(id, userId, request.getMessage());
            return ResponseEntity.ok(mapMessage(message));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            conversationService.markRead(id, userId);
            return ResponseEntity.ok().build();
        } catch (GenericException e) {
            return ResponseEntity.status(404).body(e.getErrorResponse());
        }
    }

    private ConversationResponse mapToResponse(Conversation conversation, Long userId) {
        ConversationResponse response = new ConversationResponse();
        response.setId(conversation.getId());
        response.setListingType(conversation.getListingType());
        response.setListingId(conversation.getListingId());
        response.setTitle(conversation.getTitle());
        response.setLastMessageAt(conversation.getLastMessageAt());
        response.setCreatedAt(conversation.getCreatedAt());
        response.setUpdatedAt(conversation.getUpdatedAt());

        Long otherId = userId != null && userId.equals(conversation.getParticipant1Id())
                ? conversation.getParticipant2Id()
                : conversation.getParticipant1Id();
        response.setOtherParticipantId(otherId);
        enrichParticipant(response, otherId);

        Message last = conversationService.getLastMessage(conversation.getId());
        if (last != null) {
            response.setLastMessagePreview(last.getMessage());
        }
        response.setUnreadCount(conversationService.getUnreadCount(conversation.getId(), userId));

        return response;
    }

    private void enrichParticipant(ConversationResponse response, Long otherId) {
        if (otherId == null) {
            return;
        }
        try {
            User other = userService.getUserById(otherId);
            response.setOtherParticipantName(other.getName());
            if (other.getProfilePhoto() != null && other.getProfilePhoto().length > 0) {
                String photoUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/user/")
                        .path(String.valueOf(otherId))
                        .path("/picture")
                        .toUriString();
                response.setOtherParticipantPhotoUrl(photoUrl);
            }
        } catch (GenericException ignored) {
            // participant missing; leave fields null
        }
    }

    private MessageResponse mapMessage(Message message) {
        MessageResponse response = new MessageResponse();
        response.setId(message.getId());
        response.setConversationId(message.getConversationId());
        response.setSenderId(message.getSenderId());
        response.setMessage(message.getMessage());
        response.setCreatedAt(message.getCreatedAt());
        response.setRead(message.isRead());
        return response;
    }
}
