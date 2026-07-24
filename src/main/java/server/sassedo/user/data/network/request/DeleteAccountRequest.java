package server.sassedo.user.data.network.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteAccountRequest {

    @NotBlank
    @Size(max = 120)
    private String currentPassword;
}
