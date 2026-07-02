package server.sassedo.user.data.network.response;

import lombok.Getter;
import lombok.Setter;
import server.sassedo.user.data.dto.ERole;

@Setter
@Getter
public class UserRolesResponse {
    private Long id;
    private ERole role;

    public UserRolesResponse(Long id, ERole role) {
        this.id = id;
        this.role = role;
    }

}
