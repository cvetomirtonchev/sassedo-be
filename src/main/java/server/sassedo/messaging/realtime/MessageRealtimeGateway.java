package server.sassedo.messaging.realtime;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Transport-neutral seam for realtime delivery. The single-instance deployment uses
 * {@link InMemorySseGateway}; a future multi-instance deployment can provide a Redis-backed
 * implementation that fans events across nodes without changing service or controller code.
 */
public interface MessageRealtimeGateway {

    /** Registers a new SSE subscription for the user and returns the emitter to hand back to Spring MVC. */
    SseEmitter subscribe(Long userId);

    /** Delivers an event to every active connection the user currently has on this instance. */
    void publishToUser(Long userId, MessageEvent event);
}
