package server.sassedo.common.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import server.sassedo.common.data.dto.ContactMessage;
import server.sassedo.common.data.network.request.ContactMessageRequest;
import server.sassedo.common.data.network.request.UpdateContactStatusRequest;
import server.sassedo.common.data.network.response.ContactMessageResponse;
import server.sassedo.common.service.contact.ContactService;
import server.sassedo.model.GenericException;
import server.sassedo.user.data.UserDetailsImpl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/contact")
public class ContactController {

    @Autowired
    private ContactService contactService;

    @PostMapping
    public ResponseEntity<?> submit(@Valid @RequestBody ContactMessageRequest request) {
        String sessionEmail = null;
        String sessionName = null;
        Long userId = null;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl user) {
            sessionEmail = user.getEmail();
            sessionName = user.getName();
            userId = user.getId();
        }

        try {
            ContactMessage message = contactService.submit(request, sessionEmail, sessionName, userId);
            return ResponseEntity.ok(convertToResponse(message));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @GetMapping("/admin/all")
    public ResponseEntity<?> getAll() {
        List<ContactMessageResponse> messages = contactService.getAll()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/admin/unseen-count")
    public ResponseEntity<?> getUnseenCount() {
        return ResponseEntity.ok(Map.of("count", contactService.countUnseen()));
    }

    @PutMapping("/admin/mark-seen")
    public ResponseEntity<?> markSeen(@RequestBody UpdateContactStatusRequest request) {
        List<ContactMessageResponse> messages = contactService.markSeen(request.getIds())
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(messages);
    }

    private ContactMessageResponse convertToResponse(ContactMessage message) {
        return new ContactMessageResponse(
                message.getId(),
                message.getName(),
                message.getEmail(),
                message.getSubject(),
                message.getMessage(),
                message.getStatus(),
                message.getCreatedAt()
        );
    }
}
