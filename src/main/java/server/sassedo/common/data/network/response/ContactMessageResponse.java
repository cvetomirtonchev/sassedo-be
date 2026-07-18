package server.sassedo.common.data.network.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import server.sassedo.common.data.dto.ContactMessageStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class ContactMessageResponse {
    private Long id;
    private String name;
    private String email;
    private String subject;
    private String message;
    private ContactMessageStatus status;
    private LocalDateTime createdAt;
}
