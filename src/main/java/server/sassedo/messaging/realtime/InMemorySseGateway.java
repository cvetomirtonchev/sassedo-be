package server.sassedo.messaging.realtime;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Single-instance realtime gateway backed by in-memory {@link SseEmitter}s keyed by user id. Emitters
 * are async, so a held stream does not pin a Tomcat worker thread. Connections are capped per user,
 * evicted on completion/timeout/error, and kept alive with periodic heartbeats.
 */
@Component
public class InMemorySseGateway implements MessageRealtimeGateway {

    private final Map<Long, Collection<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final AtomicInteger activeConnections = new AtomicInteger(0);

    private final MeterRegistry meterRegistry;
    private Counter eventsSent;
    private Counter eventsFailed;

    @Value("${sassedo.messaging.sse.timeout-ms:1800000}")
    private long timeoutMs;

    @Value("${sassedo.messaging.sse.max-connections-per-user:5}")
    private int maxConnectionsPerUser;

    public InMemorySseGateway(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    void initMetrics() {
        Gauge.builder("messaging.sse.active_connections", activeConnections, AtomicInteger::get)
                .description("Active messaging SSE connections on this instance")
                .register(meterRegistry);
        eventsSent = Counter.builder("messaging.sse.events_sent")
                .description("Realtime events successfully written to a client")
                .register(meterRegistry);
        eventsFailed = Counter.builder("messaging.sse.events_failed")
                .description("Realtime events that failed to write (broken connection)")
                .register(meterRegistry);
    }

    @Override
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(timeoutMs);
        Collection<SseEmitter> userEmitters =
                emitters.computeIfAbsent(userId, key -> new CopyOnWriteArrayList<>());

        enforceConnectionLimit(userEmitters);

        userEmitters.add(emitter);
        activeConnections.incrementAndGet();

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            remove(userId, emitter);
        });
        emitter.onError(e -> remove(userId, emitter));

        return emitter;
    }

    @Override
    public void publishToUser(Long userId, MessageEvent event) {
        Collection<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .id(event.getEventId())
                        .name(event.eventName())
                        .data(event, MediaType.APPLICATION_JSON));
                eventsSent.increment();
            } catch (IOException | IllegalStateException e) {
                eventsFailed.increment();
                emitter.completeWithError(e);
                remove(userId, emitter);
            }
        }
    }

    @Scheduled(fixedRateString = "${sassedo.messaging.sse.heartbeat-ms:20000}")
    void heartbeat() {
        for (Map.Entry<Long, Collection<SseEmitter>> entry : emitters.entrySet()) {
            for (SseEmitter emitter : entry.getValue()) {
                try {
                    emitter.send(SseEmitter.event().comment("keepalive"));
                } catch (IOException | IllegalStateException e) {
                    emitter.completeWithError(e);
                    remove(entry.getKey(), emitter);
                }
            }
        }
    }

    public int activeCount() {
        return activeConnections.get();
    }

    private void enforceConnectionLimit(Collection<SseEmitter> userEmitters) {
        if (maxConnectionsPerUser <= 0) {
            return;
        }
        Iterator<SseEmitter> iterator = userEmitters.iterator();
        while (userEmitters.size() >= maxConnectionsPerUser && iterator.hasNext()) {
            SseEmitter oldest = iterator.next();
            try {
                oldest.complete();
            } catch (Exception ignored) {
                // best-effort eviction; removal happens via completion callback
            }
        }
    }

    private void remove(Long userId, SseEmitter emitter) {
        Collection<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters != null && userEmitters.remove(emitter)) {
            activeConnections.decrementAndGet();
            if (userEmitters.isEmpty()) {
                emitters.remove(userId, userEmitters);
            }
        }
    }
}
