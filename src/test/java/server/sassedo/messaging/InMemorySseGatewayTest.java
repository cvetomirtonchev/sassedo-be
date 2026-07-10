package server.sassedo.messaging;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import server.sassedo.messaging.realtime.InMemorySseGateway;
import server.sassedo.messaging.realtime.MessageEvent;

import static org.assertj.core.api.Assertions.assertThat;

class InMemorySseGatewayTest {

    private SimpleMeterRegistry registry;
    private InMemorySseGateway gateway;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        gateway = new InMemorySseGateway(registry);
        ReflectionTestUtils.setField(gateway, "timeoutMs", 60_000L);
        ReflectionTestUtils.setField(gateway, "maxConnectionsPerUser", 5);
        ReflectionTestUtils.invokeMethod(gateway, "initMetrics");
    }

    @Test
    void subscribeRegistersConnection() {
        SseEmitter emitter = gateway.subscribe(1L);

        assertThat(emitter).isNotNull();
        assertThat(gateway.activeCount()).isEqualTo(1);
        assertThat(registry.get("messaging.sse.active_connections").gauge().value()).isEqualTo(1.0);
    }

    @Test
    void publishToSubscribedUserSendsEvent() {
        gateway.subscribe(1L);

        gateway.publishToUser(1L, MessageEvent.unreadUpdated(3L));

        assertThat(registry.get("messaging.sse.events_sent").counter().count()).isEqualTo(1.0);
    }

    @Test
    void publishToUnknownUserIsNoop() {
        gateway.publishToUser(999L, MessageEvent.unreadUpdated(1L));

        assertThat(registry.get("messaging.sse.events_sent").counter().count()).isZero();
    }
}
