package server.sassedo.listing.roommate.data.network.request;

import lombok.Getter;
import lombok.Setter;
import server.sassedo.listing.common.*;
import server.sassedo.user.data.dto.Language;
import server.sassedo.user.data.dto.Occupation;
import server.sassedo.user.data.dto.Sex;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
public class RoommateListingRequest {

    // Whether the lister has a property (true) or is looking for a place to share (false).
    private Boolean hasProperty;

    // Monthly budget for listers without a property.
    private BigDecimal budget;

    // Step 1
    private PropertyType propertyType;

    // Step 2
    private Long countryId;
    private Long cityId;
    private String neighborhood;
    private String address;

    // Step 3
    private BigDecimal rent;
    private Boolean costsIncluded;
    private BigDecimal deposit;
    private Integer roomsCount;
    private Boolean furnished;
    private Boolean petsAllowed;
    private LocalDate availableFrom;
    private boolean availableAsap;
    private Integer bedrooms;
    private Integer bathrooms;
    private Boolean sharedBedroom;
    private Boolean sharedBathroom;
    private Integer areaSqm;
    private RoomArrangement roomArrangement;
    private Boolean owner;

    private Set<Utility> includedUtilities = new LinkedHashSet<>();
    private Set<ExtraService> extraServices = new LinkedHashSet<>();
    private Set<NearbyAmenity> nearbyAmenities = new LinkedHashSet<>();
    private Set<RoomAmenity> roomAmenities = new LinkedHashSet<>();

    // Step 4
    private Sex preferredSex;
    private String preferredOrientation;
    private Integer ageMin;
    private Integer ageMax;
    private SmokerPreference smokingPreference;
    private String additionalRequirements;
    private PetPolicy petPolicy;
    private Set<Language> spokenLanguages = new LinkedHashSet<>();
    private Occupation employmentStatus;
    private Boolean hasChildren;
    private String aboutMe;

    // Step 6
    private String title;
    private String description;
}
