package server.sassedo.user.data.network.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordCodeRequest {
    private String email;
}
