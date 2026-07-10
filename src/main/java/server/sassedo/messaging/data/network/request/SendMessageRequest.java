package server.sassedo.messaging.data.network.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendMessageRequest {

    @NotBlank
    @Size(max = 5000)
    private String message;

    /** Optional client-generated idempotency key so retried sends don't create duplicates. */
    @Size(max = 64)
    private String clientMessageId;
}
