package server.sassedo.user.data.projection;

import server.sassedo.listing.common.PetPolicy;
import server.sassedo.listing.common.SmokerPreference;
import server.sassedo.user.data.dto.Occupation;
import server.sassedo.user.data.dto.Sex;

/**
 * Spring Data projection exposing only the publicly shareable profile attributes of a listing
 * owner. Deliberately never selects the {@code profilePhoto} MEDIUMBLOB (presence is a boolean) and
 * omits private fields such as email, phone, roles and preferences.
 */
public interface PublicProfileView {

    Long getId();

    String getName();

    Integer getAge();

    Sex getSex();

    Occupation getOccupation();

    SmokerPreference getSmokingPreference();

    PetPolicy getPetPolicy();

    String getShortDescription();

    boolean getHasPhoto();
}
