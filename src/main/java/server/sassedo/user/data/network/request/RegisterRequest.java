package server.sassedo.user.data.network.request;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Getter
@Setter
public class RegisterRequest {
    @NotBlank
    @Size(max = 100)
    @Email
    private String email;

    @NotBlank
    @Size(max = 120)
    private String password;

    @NotBlank
    @Size(min = 2, max = 60)
    private String firstName;

    @NotBlank
    @Size(min = 2, max = 60)
    private String lastName;

    @NotBlank
    @Size(max = 30)
    @Pattern(regexp = "^\\+[1-9]\\d{6,14}$", message = "Phone must be a valid international number")
    private String phone;

    private boolean acceptedTerms;

    private boolean acceptedGdpr;
}
