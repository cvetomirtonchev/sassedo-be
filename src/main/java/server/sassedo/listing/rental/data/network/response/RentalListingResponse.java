package server.sassedo.listing.rental.data.network.response;

import lombok.Getter;
import lombok.Setter;
import server.sassedo.engagement.service.EngagementAware;
import server.sassedo.listing.common.*;
import server.sassedo.listing.common.matching.PreferenceMatchResponse;
import server.sassedo.listing.roommate.data.network.response.ListingPhotoResponse;
import server.sassedo.promotion.common.PromotionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class RentalListingResponse implements EngagementAware {

    private Long id;
    private Long ownerId;
    private String ownerName;
    private String ownerPhotoUrl;
    private ListingStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;

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

    private PromotionType promotionType;
    private int promotionPriority;
    private LocalDateTime promotedUntil;

    private Integer matchScore;

    // Which property details match the requesting user's saved preferences. Only populated for
    // authenticated requests; null otherwise.
    private PreferenceMatchResponse preferenceMatch;

    private long favoriteCount;
    private long viewCount;
    private boolean favoritedByMe;

    private int editCount;
    private int maxEdits;
    private int remainingEdits;
    private String rejectionReason;
}
