package server.sassedo.common.data.network.request;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
public class ContactMessageRequest {
    @NotBlank
    private String name;

    @Email
    private String email;

    @NotBlank
    private String subject;

    @NotBlank
    private String message;
}
