package server.sassedo.listing.roommate.data.network.response;

import lombok.Getter;
import lombok.Setter;
import server.sassedo.listing.common.PetPolicy;
import server.sassedo.listing.common.SmokerPreference;
import server.sassedo.user.data.dto.Language;
import server.sassedo.user.data.dto.Occupation;
import server.sassedo.user.data.dto.Sex;

import java.util.Set;

/**
 * Publicly shareable subset of a listing owner's profile, surfaced on the roommate detail page so
 * viewers can learn about the person living in (or looking for) the property. Only exposes safe
 * attributes; never carries email, phone, roles or saved preferences.
 */
@Getter
@Setter
public class OwnerProfileResponse {

    private Integer age;
    private Sex sex;
    private PetPolicy petPolicy;
    private SmokerPreference smokingPreference;
    private Set<Language> languages;
    private Occupation employmentStatus;
    private String aboutMe;
}
