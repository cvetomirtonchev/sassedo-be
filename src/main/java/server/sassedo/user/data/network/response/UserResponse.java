package server.sassedo.user.data.network.response;

import lombok.Getter;
import lombok.Setter;
import server.sassedo.listing.common.NearbyAmenity;
import server.sassedo.listing.common.PetPolicy;
import server.sassedo.listing.common.PropertyType;
import server.sassedo.listing.common.RoomAmenity;
import server.sassedo.listing.common.SmokerPreference;
import server.sassedo.user.data.dto.ERole;
import server.sassedo.user.data.dto.JobStatus;
import server.sassedo.user.data.dto.Language;
import server.sassedo.user.data.dto.Occupation;
import server.sassedo.user.data.dto.Sex;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class UserResponse {
    private Long id;

    private String email;

    private String name;

    private String firstName;

    private String lastName;

    private String phone;

    private boolean isVerified;

    private boolean blocked;

    private List<ERole> roles;

    private boolean isMarketingConsentAccepted;

    private String profilePhotoUrl;

    private Integer age;

    private Sex sex;

    private Set<Language> languages;

    private JobStatus jobStatus;

    private String profession;

    private SmokerPreference smokingPreference;

    private PetPolicy petPolicy;

    private Occupation occupation;

    private String shortDescription;

    private BigDecimal preferredMaxBudget;

    private PropertyType preferredPropertyType;

    private Boolean preferredFurnished;

    private Boolean preferredPetsAllowed;

    private Integer preferredMinBedrooms;

    private Integer preferredMinBathrooms;

    private Long preferredCountryId;

    private String preferredCountryNameEn;

    private String preferredCountryNameBg;

    private Long preferredCityId;

    private String preferredCityNameEn;

    private String preferredCityNameBg;

    private Set<RoomAmenity> preferredRoomAmenities;

    private Set<NearbyAmenity> preferredNearbyAmenities;

    private boolean profileComplete;

    private boolean preferencesComplete;

    public UserResponse(Long id, String email, String name, boolean isVerified, List<ERole> roles, boolean isMarketingConsentAccepted) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.isVerified = isVerified;
        this.roles = roles;
        this.isMarketingConsentAccepted = isMarketingConsentAccepted;
    }

    public UserResponse(Long id, String email, String name) {
        this.id = id;
        this.email = email;
        this.name = name;
    }
}
