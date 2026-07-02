package server.sassedo.user.data.network.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserPreferencesRequest {
    private Boolean marketingConsentAccepted;
    // Future preferences go here as additional Boolean fields
}
