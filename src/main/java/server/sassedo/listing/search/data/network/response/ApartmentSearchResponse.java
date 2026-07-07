package server.sassedo.listing.search.data.network.response;

import lombok.Getter;
import lombok.Setter;
import server.sassedo.listing.common.LeaseTerm;
import server.sassedo.listing.common.ListingStatus;
import server.sassedo.listing.common.PropertyType;
import server.sassedo.listing.common.SmokerPreference;
import server.sassedo.promotion.common.PromotionType;
import server.sassedo.user.data.dto.Sex;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
public class ApartmentSearchResponse {

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

    private BigDecimal budgetMin;
    private BigDecimal budgetMax;

    private LocalDate availableFrom;
    private boolean availableAsap;

    private Set<LeaseTerm> leaseTerms;

    private Integer age;
    private Sex sex;
    private String profession;
    private SmokerPreference smoker;
    private Boolean hasPets;

    private String title;
    private String description;

    private PromotionType promotionType;
    private int promotionPriority;
    private LocalDateTime promotedUntil;
    private boolean pinned;
}
