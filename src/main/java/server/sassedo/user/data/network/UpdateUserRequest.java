package server.sassedo.user.data.network;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {
    private Long userId;
    private String email;
}
