package server.sassedo.listing.roommate.data.network.response;

import lombok.Getter;
import lombok.Setter;
import server.sassedo.engagement.service.EngagementAware;
import server.sassedo.listing.common.*;
import server.sassedo.promotion.common.PromotionType;
import server.sassedo.user.data.dto.JobStatus;
import server.sassedo.user.data.dto.Language;
import server.sassedo.user.data.dto.Sex;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class RoommateListingResponse implements EngagementAware {

    private Long id;
    private Long ownerId;
    private String ownerName;
    private String ownerPhotoUrl;
    private ListingStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;

    private Boolean hasProperty;
    private BigDecimal budget;

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
    private Boolean costsIncluded;
    private BigDecimal deposit;
    private Integer roomsCount;
    private Boolean furnished;
    private Boolean petsAllowed;
    private LocalDate availableFrom;
    private boolean availableAsap;
    private Integer bedrooms;
    private Integer bathrooms;
    private Boolean owner;

    private Set<Utility> includedUtilities;
    private Set<ExtraService> extraServices;
    private Set<NearbyAmenity> nearbyAmenities;
    private Set<RoomAmenity> roomAmenities;

    private Sex preferredSex;
    private String preferredOrientation;
    private Integer ageMin;
    private Integer ageMax;
    private SmokerPreference smokingPreference;
    private OccupationPreference occupationPreference;
    private String additionalRequirements;
    private PetPolicy petPolicy;
    private Integer peopleInProperty;
    private Set<Language> spokenLanguages;
    private JobStatus employmentStatus;
    private String aboutMe;

    private String title;
    private String description;

    private String mainPhotoUrl;
    private List<ListingPhotoResponse> photos;

    private PromotionType promotionType;
    private int promotionPriority;
    private LocalDateTime promotedUntil;
    private boolean pinned;

    // Compatibility (0-100) between the requesting user's profile and this listing's roommate
    // requirements. Only populated for authenticated requests; null otherwise.
    private Integer matchScore;

    private long favoriteCount;
    private long viewCount;
    private boolean favoritedByMe;
}
