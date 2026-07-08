package server.sassedo.user.data.dto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import server.sassedo.listing.common.NearbyAmenity;
import server.sassedo.listing.common.PropertyType;
import server.sassedo.listing.common.RoomAmenity;
import server.sassedo.location.data.dto.City;
import server.sassedo.location.data.dto.Country;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Optional roommate/property preferences describing the kind of place the user is looking for.
 * Embedded into {@link User}; used to enrich listing match scoring. All fields are optional and do
 * not affect profile completeness.
 */
@Embeddable
@Getter
@Setter
public class UserPreferencesDto {

    @Column(name = "preferred_max_budget")
    private BigDecimal preferredMaxBudget;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_property_type")
    private PropertyType preferredPropertyType;

    @Column(name = "preferred_furnished")
    private Boolean preferredFurnished;

    @Column(name = "preferred_pets_allowed")
    private Boolean preferredPetsAllowed;

    @Column(name = "preferred_min_bedrooms")
    private Integer preferredMinBedrooms;

    @Column(name = "preferred_min_bathrooms")
    private Integer preferredMinBathrooms;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_country_id")
    private Country preferredCountry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_city_id")
    private City preferredCity;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_preferred_room_amenities", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "amenity")
    private Set<RoomAmenity> preferredRoomAmenities = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_preferred_nearby_amenities", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "amenity")
    private Set<NearbyAmenity> preferredNearbyAmenities = new LinkedHashSet<>();
}
