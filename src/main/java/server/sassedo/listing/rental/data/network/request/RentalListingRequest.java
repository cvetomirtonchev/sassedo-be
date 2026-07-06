package server.sassedo.listing.rental.data.network.request;

import lombok.Getter;
import lombok.Setter;
import server.sassedo.listing.common.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
public class RentalListingRequest {

    private PropertyType propertyType;

    private Long countryId;
    private Long cityId;
    private String neighborhood;
    private String address;

    private BigDecimal rent;
    private LocalDate availableFrom;
    private boolean availableAsap;

    private Integer bedrooms;
    private Integer bathrooms;
    private Boolean sharedBedroom;
    private Boolean sharedBathroom;
    private Boolean owner;

    private PetPolicy petPolicy;
    private SmokingPolicy smokingPolicy;

    private Set<Utility> includedUtilities = new LinkedHashSet<>();
    private Set<ExtraService> extraServices = new LinkedHashSet<>();
    private Set<LeaseTerm> leaseTerms = new LinkedHashSet<>();
    private Set<NearbyAmenity> nearbyAmenities = new LinkedHashSet<>();
    private Set<PropertyAmenity> propertyAmenities = new LinkedHashSet<>();

    private String additionalDetails;
    private String title;
    private String description;
}
