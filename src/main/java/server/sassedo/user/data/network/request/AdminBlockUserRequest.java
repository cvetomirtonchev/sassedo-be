package server.sassedo.user.data.network.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminBlockUserRequest {
    private Long userId;
    private boolean blocked;
}
