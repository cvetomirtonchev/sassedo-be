package server.sassedo.user.data.network.response;

import lombok.Getter;
import lombok.Setter;
import server.sassedo.user.data.dto.ERole;

import java.util.List;

@Getter
@Setter
public class UserResponse {
    private Long id;

    private String email;

    private String name;

    private boolean isVerified;

    private List<ERole> roles;

    private boolean isMarketingConsentAccepted;

    public UserResponse(Long id, String email, String name, boolean isVerified, List<ERole> roles, boolean isMarketingConsentAccepted) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.isVerified = isVerified;
        this.roles = roles;
        this.isMarketingConsentAccepted = isMarketingConsentAccepted;
    }

    public UserResponse(Long id, String email, String name) {
        this.id = id;
        this.email = email;
        this.name = name;
    }
}
