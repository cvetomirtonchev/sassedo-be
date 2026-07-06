package server.sassedo.user.data.network.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import server.sassedo.listing.common.PetPolicy;
import server.sassedo.listing.common.SmokerPreference;
import server.sassedo.user.data.dto.JobStatus;
import server.sassedo.user.data.dto.Language;
import server.sassedo.user.data.dto.Occupation;
import server.sassedo.user.data.dto.Sex;

import java.util.Set;

@Getter
@Setter
public class AdminUpdateUserRequest {

    private Long userId;

    @Size(max = 50)
    private String email;

    @Size(max = 60)
    private String firstName;

    @Size(max = 60)
    private String lastName;

    @Size(max = 30)
    private String phone;

    private Boolean verified;

    @Size(max = 100)
    private String city;

    @Min(16)
    @Max(120)
    private Integer age;

    private Sex sex;

    private Set<Language> languages;

    private JobStatus jobStatus;

    @Size(max = 100)
    private String profession;

    private SmokerPreference smokingPreference;

    private PetPolicy petPolicy;

    private Occupation occupation;

    @Size(max = 1000)
    private String shortDescription;
}
