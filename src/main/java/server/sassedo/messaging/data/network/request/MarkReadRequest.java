package server.sassedo.messaging.data.network.request;

import lombok.Getter;
import lombok.Setter;

/**
 * Marks a conversation read up to an explicit message id. When {@code upToMessageId} is null the whole
 * conversation is marked read. Passing an explicit id avoids accidentally clearing a message that
 * arrived after the client rendered its view.
 */
@Getter
@Setter
public class MarkReadRequest {
    private Long upToMessageId;
}
