package server.sassedo.listing.common;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * Optional browse filters shared across listing types. Every field is nullable/empty when not
 * applied; specifications only add a predicate for the fields that are present. Not all fields are
 * relevant to every listing type (e.g. apartment searches have no bedrooms), so unsupported ones are
 * simply left null by the corresponding controller.
 */
@Getter
@Setter
public class ListingFilter {

    private Long cityId;
    // Roommate browse only: filter by whether the listing has a property. Null means no filter.
    private Boolean hasProperty;
    private PropertyType propertyType;
    private String neighborhood;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private LocalDate availableBy;
    private Integer minBedrooms;
    private Integer minBathrooms;
    private Set<LeaseTerm> leaseTerms;
    private Set<String> amenities;
}
