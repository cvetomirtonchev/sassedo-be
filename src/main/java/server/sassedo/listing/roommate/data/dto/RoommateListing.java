package server.sassedo.listing.roommate.data.dto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import server.sassedo.listing.common.*;
import server.sassedo.location.data.dto.City;
import server.sassedo.location.data.dto.Country;
import server.sassedo.user.data.dto.JobStatus;
import server.sassedo.user.data.dto.Language;
import server.sassedo.user.data.dto.Sex;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "roommate_listings")
public class RoommateListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ownership / meta
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListingStatus status = ListingStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Step 1: property type
    @Enumerated(EnumType.STRING)
    private PropertyType propertyType;

    // Step 2: location
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id")
    private Country country;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private City city;

    private String neighborhood;

    private String address;

    // Step 3: property details
    private BigDecimal rent;

    private Boolean costsIncluded;

    private BigDecimal deposit;

    private Integer roomsCount;

    private Boolean furnished;

    private Boolean petsAllowed;

    @Column(name = "available_from")
    private LocalDate availableFrom;

    @Column(name = "available_asap", nullable = false)
    private boolean availableAsap = false;

    private Integer bedrooms;

    private Integer bathrooms;

    @Column(name = "is_owner")
    private Boolean owner;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "roommate_listing_included_utilities", joinColumns = @JoinColumn(name = "listing_id"))
    @Column(name = "utility")
    private Set<Utility> includedUtilities = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "roommate_listing_extra_services", joinColumns = @JoinColumn(name = "listing_id"))
    @Column(name = "service")
    private Set<ExtraService> extraServices = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "roommate_listing_nearby_amenities", joinColumns = @JoinColumn(name = "listing_id"))
    @Column(name = "amenity")
    private Set<NearbyAmenity> nearbyAmenities = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "roommate_listing_room_amenities", joinColumns = @JoinColumn(name = "listing_id"))
    @Column(name = "amenity")
    private Set<RoomAmenity> roomAmenities = new LinkedHashSet<>();

    // Step 4: roommate requirements
    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_sex")
    private Sex preferredSex;

    @Column(name = "preferred_orientation")
    private String preferredOrientation;

    @Column(name = "age_min")
    private Integer ageMin;

    @Column(name = "age_max")
    private Integer ageMax;

    @Enumerated(EnumType.STRING)
    @Column(name = "smoking_preference")
    private SmokerPreference smokingPreference;

    @Enumerated(EnumType.STRING)
    @Column(name = "occupation_preference")
    private OccupationPreference occupationPreference;

    @Lob
    @Column(name = "additional_requirements", columnDefinition = "TEXT")
    private String additionalRequirements;

    @Enumerated(EnumType.STRING)
    @Column(name = "pet_policy")
    private PetPolicy petPolicy;

    @Column(name = "people_in_property")
    private Integer peopleInProperty;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "roommate_listing_spoken_languages", joinColumns = @JoinColumn(name = "listing_id"))
    @Column(name = "language")
    private Set<Language> spokenLanguages = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status")
    private JobStatus employmentStatus;

    @Lob
    @Column(name = "about_me", columnDefinition = "TEXT")
    private String aboutMe;

    // Step 6: title & description
    private String title;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoommateListingPhoto> photos = new ArrayList<>();

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.status == null) {
            this.status = ListingStatus.PENDING;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
