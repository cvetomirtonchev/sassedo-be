package server.sassedo.listing.rental.data.dto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import server.sassedo.listing.common.*;
import server.sassedo.location.data.dto.City;
import server.sassedo.location.data.dto.Country;

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
@Table(name = "rental_listings")
public class RentalListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListingStatus status = ListingStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private PropertyType propertyType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id")
    private Country country;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private City city;

    private String neighborhood;

    private String address;

    private BigDecimal rent;

    @Column(name = "available_from")
    private LocalDate availableFrom;

    @Column(name = "available_asap", nullable = false)
    private boolean availableAsap = false;

    private Integer bedrooms;

    private Integer bathrooms;

    @Column(name = "shared_bedroom")
    private Boolean sharedBedroom;

    @Column(name = "shared_bathroom")
    private Boolean sharedBathroom;

    @Column(name = "is_owner")
    private Boolean owner;

    @Enumerated(EnumType.STRING)
    @Column(name = "pet_policy")
    private PetPolicy petPolicy;

    @Enumerated(EnumType.STRING)
    @Column(name = "smoking_policy")
    private SmokingPolicy smokingPolicy;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "rental_listing_included_utilities", joinColumns = @JoinColumn(name = "listing_id"))
    @Column(name = "utility")
    private Set<Utility> includedUtilities = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "rental_listing_extra_services", joinColumns = @JoinColumn(name = "listing_id"))
    @Column(name = "service")
    private Set<ExtraService> extraServices = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "rental_listing_lease_terms", joinColumns = @JoinColumn(name = "listing_id"))
    @Column(name = "lease_term")
    private Set<LeaseTerm> leaseTerms = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "rental_listing_nearby_amenities", joinColumns = @JoinColumn(name = "listing_id"))
    @Column(name = "amenity")
    private Set<NearbyAmenity> nearbyAmenities = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "rental_listing_property_amenities", joinColumns = @JoinColumn(name = "listing_id"))
    @Column(name = "amenity")
    private Set<PropertyAmenity> propertyAmenities = new LinkedHashSet<>();

    @Lob
    @Column(name = "additional_details", columnDefinition = "TEXT")
    private String additionalDetails;

    private String title;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RentalListingPhoto> photos = new ArrayList<>();

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
