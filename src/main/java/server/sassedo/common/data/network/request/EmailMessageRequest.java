package server.sassedo.common.data.network.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailMessageRequest {
    private String subject;
    private String content;
    private String recipient;
}
