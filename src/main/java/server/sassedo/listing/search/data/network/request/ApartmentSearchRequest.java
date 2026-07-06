package server.sassedo.listing.search.data.network.request;

import lombok.Getter;
import lombok.Setter;
import server.sassedo.listing.common.LeaseTerm;
import server.sassedo.listing.common.PropertyType;
import server.sassedo.listing.common.SmokerPreference;
import server.sassedo.user.data.dto.Sex;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
public class ApartmentSearchRequest {

    private PropertyType propertyType;

    private Long countryId;
    private Long cityId;
    private String neighborhood;

    private BigDecimal budgetMin;
    private BigDecimal budgetMax;

    private LocalDate availableFrom;
    private boolean availableAsap;

    private Set<LeaseTerm> leaseTerms = new LinkedHashSet<>();

    private Integer age;
    private Sex sex;
    private String profession;
    private SmokerPreference smoker;
    private Boolean hasPets;

    private String title;
    private String description;
}
