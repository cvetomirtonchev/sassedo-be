package server.sassedo.listing.common.matching;

import server.sassedo.location.data.dto.City;
import server.sassedo.location.data.dto.Country;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Set;

/**
 * Shared per-dimension scoring helpers used by the listing match scorers. Each method returns a
 * value in [0,1]; callers apply their own weights and applicability rules.
 */
public final class ListingMatchSupport {

    // Fraction of overage above the budget before rent scores 0 (e.g. 0.2 = 20% over budget).
    private static final double BUDGET_FALLOFF_RATIO = 0.2;

    private ListingMatchSupport() {
    }

    /**
     * 1.0 when rent is within budget, then a linear falloff for small overages down to 0.
     */
    public static double scoreBudget(BigDecimal rent, BigDecimal maxBudget) {
        if (maxBudget.signum() <= 0) {
            return rent.compareTo(maxBudget) <= 0 ? 1.0 : 0.0;
        }
        if (rent.compareTo(maxBudget) <= 0) {
            return 1.0;
        }
        double overageRatio = rent.subtract(maxBudget)
                .divide(maxBudget, MathContext.DECIMAL64)
                .doubleValue();
        return Math.max(0.0, 1.0 - (overageRatio / BUDGET_FALLOFF_RATIO));
    }

    /**
     * Fraction of the user's preferred amenities that the listing offers.
     */
    public static <T> double amenityOverlap(Set<T> preferred, Set<T> available) {
        if (preferred.isEmpty()) {
            return 0.0;
        }
        long overlap = preferred.stream().filter(available::contains).count();
        return (double) overlap / preferred.size();
    }

    /**
     * 1.0 for an exact city match, 0.5 when only the country matches, else 0.0. The desired country
     * falls back to the preferred city's country when no explicit country preference is set.
     */
    public static double scoreLocation(City preferredCity, Country preferredCountry,
            City listingCity, Country listingCountry) {
        Long desiredCityId = preferredCity != null ? preferredCity.getId() : null;
        Long desiredCountryId = preferredCountry != null ? preferredCountry.getId()
                : (preferredCity != null && preferredCity.getCountry() != null
                        ? preferredCity.getCountry().getId() : null);

        if (desiredCityId != null && listingCity != null
                && desiredCityId.equals(listingCity.getId())) {
            return 1.0;
        }
        if (desiredCountryId != null && listingCountry != null
                && desiredCountryId.equals(listingCountry.getId())) {
            return 0.5;
        }
        return 0.0;
    }
}
