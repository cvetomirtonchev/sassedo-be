package server.sassedo.listing.common;

import lombok.Getter;
import lombok.Setter;
import server.sassedo.user.data.dto.Language;
import server.sassedo.user.data.dto.Occupation;
import server.sassedo.user.data.dto.Sex;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * Optional browse filters shared across listing types. Every field is nullable/empty when not
 * applied; specifications only add a predicate for the fields that are present. Not all fields are
 * relevant to every listing type (e.g. apartment searches have no bedrooms), so unsupported ones are
 * simply left null by the corresponding controller.
 */
@Getter
@Setter
public class ListingFilter {

    private Long cityId;
    // Roommate browse only: filter by whether the listing has a property. Null means no filter.
    private Boolean hasProperty;
    private PropertyType propertyType;
    private String neighborhood;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private LocalDate availableBy;
    // When true, restrict to listings marked available as soon as possible.
    private Boolean availableAsap;
    private Integer minBedrooms;
    private Integer minBathrooms;
    private Set<LeaseTerm> leaseTerms;
    private Set<String> amenities;

    // Roommate-only person/lifestyle filters. Match the listing's stated roommate preferences.
    private Integer ageMin;
    private Integer ageMax;
    private Sex preferredSex;
    // Roommate pets: with-property maps to petsAllowed, without-property to petPolicy != NOT_ALLOWED.
    private Boolean petsAllowed;
    private SmokerPreference smokingPreference;
    private Occupation employmentStatus;
    private Set<Language> spokenLanguages;
    // Roommate with-property: how the offered space is arranged.
    private RoomArrangement roomArrangement;
    // Roommate without-property lifestyle: whether the lister has children.
    private Boolean hasChildren;

    // Rental and roommate with-property: willingness to use a shared bedroom/bathroom.
    private Boolean sharedBedroom;
    private Boolean sharedBathroom;
}
