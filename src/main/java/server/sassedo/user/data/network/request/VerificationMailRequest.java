package server.sassedo.user.data.network.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerificationMailRequest {
    private String email;
}
