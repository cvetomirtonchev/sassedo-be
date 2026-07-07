package server.sassedo.messaging.data.network.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class MessageResponse {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String message;
    private LocalDateTime createdAt;
    private boolean isRead;
}
