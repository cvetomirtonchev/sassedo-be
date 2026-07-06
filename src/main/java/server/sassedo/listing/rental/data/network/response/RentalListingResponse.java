package server.sassedo.listing.rental.data.network.response;

import lombok.Getter;
import lombok.Setter;
import server.sassedo.listing.common.*;
import server.sassedo.listing.roommate.data.network.response.ListingPhotoResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class RentalListingResponse {

    private Long id;
    private Long ownerId;
    private ListingStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private PropertyType propertyType;

    private Long countryId;
    private String countryNameEn;
    private String countryNameBg;
    private Long cityId;
    private String cityNameEn;
    private String cityNameBg;
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

    private Set<Utility> includedUtilities;
    private Set<ExtraService> extraServices;
    private Set<LeaseTerm> leaseTerms;
    private Set<NearbyAmenity> nearbyAmenities;
    private Set<PropertyAmenity> propertyAmenities;

    private String additionalDetails;
    private String title;
    private String description;

    private String mainPhotoUrl;
    private List<ListingPhotoResponse> photos;
}
