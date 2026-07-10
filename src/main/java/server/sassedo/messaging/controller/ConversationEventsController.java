package server.sassedo.messaging.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import server.sassedo.messaging.realtime.MessageRealtimeGateway;
import server.sassedo.security.jwt.JwtUtils;

import static server.sassedo.utils.ServerUtils.getUserId;

/**
 * Server-Sent Events stream for realtime messaging. Authenticated through the standard Bearer header
 * (the frontend uses a fetch-based reader, not the native {@code EventSource}, so it can attach the
 * Authorization header). One stream per browser tab; the client reconnects and reconciles on drop.
 *
 * IMPORTANT: this handler performs no JPA work on the request thread. With Open-Session-In-View enabled
 * (the app relies on it for lazy serialization elsewhere), any query here would pin the request's JDBC
 * connection for the entire, long-lived stream and quickly exhaust the pool. The client fetches the
 * current unread count over REST as part of its on-connect reconciliation instead.
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationEventsController {

    private final MessageRealtimeGateway gateway;
    private final JwtUtils jwtUtils;

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(HttpServletRequest request, HttpServletResponse response) {
        // Disable proxy buffering so events flush immediately (e.g. behind nginx).
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");

        Long userId = getUserId(request, jwtUtils);
        return gateway.subscribe(userId);
    }
}
