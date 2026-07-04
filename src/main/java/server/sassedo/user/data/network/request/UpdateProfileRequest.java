package server.sassedo.user.data.network.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import server.sassedo.user.data.dto.JobStatus;
import server.sassedo.user.data.dto.Language;
import server.sassedo.user.data.dto.Sex;

import java.util.Set;

@Getter
@Setter
public class UpdateProfileRequest {

    @Size(max = 60)
    private String firstName;

    @Size(max = 60)
    private String lastName;

    @Size(max = 30)
    @Pattern(regexp = "^\\+[1-9]\\d{6,14}$", message = "Phone must be a valid international number")
    private String phone;

    @Size(max = 100)
    private String city;

    @Min(16)
    @Max(120)
    private Integer age;

    private Sex sex;

    private Set<Language> languages;

    private JobStatus jobStatus;

    private Boolean smoker;

    private Boolean hasPets;

    @Size(max = 1000)
    private String shortDescription;
}
