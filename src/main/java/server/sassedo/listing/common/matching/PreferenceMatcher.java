package server.sassedo.listing.common.matching;

import server.sassedo.listing.common.NearbyAmenity;
import server.sassedo.listing.common.PropertyType;
import server.sassedo.listing.common.RoomAmenity;
import server.sassedo.location.data.dto.City;
import server.sassedo.location.data.dto.Country;
import server.sassedo.user.data.dto.UserPreferencesDto;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Computes which of a listing's property details satisfy the viewer's saved preferences, for
 * display-only highlighting. This intentionally reports full matches only (so the UI marks a value
 * green when it clearly matches and leaves everything else untouched); it does not affect the
 * aggregate match score computed by the per-type scorers.
 */
public final class PreferenceMatcher {

    private PreferenceMatcher() {
    }

    /**
     * @param petsAllowed resolved "the listing allows pets" flag (rentals derive this from their pet
     *                    policy; roommate listings use their {@code petsAllowed} column). May be null.
     * @param furnished   whether the listing is furnished; pass {@code null} for listing types that do
     *                    not model it (e.g. rentals).
     * @param roomAmenities the listing's room amenities, or {@code null} when not applicable.
     */
    public static PreferenceMatchResponse evaluate(UserPreferencesDto prefs,
            BigDecimal rent, PropertyType propertyType, Boolean furnished, Boolean petsAllowed,
            Integer bedrooms, Integer bathrooms, City city, Country country,
            Set<RoomAmenity> roomAmenities, Set<NearbyAmenity> nearbyAmenities) {
        PreferenceMatchResponse result = new PreferenceMatchResponse();
        if (prefs == null) {
            return result;
        }

        // Budget: the listing's rent is within the viewer's max budget.
        if (prefs.getPreferredMaxBudget() != null && rent != null
                && ListingMatchSupport.scoreBudget(rent, prefs.getPreferredMaxBudget()) >= 1.0) {
            result.setBudget(MatchState.EXACT);
        }

        // Property type: exact enum match.
        if (prefs.getPreferredPropertyType() != null && propertyType != null
                && propertyType == prefs.getPreferredPropertyType()) {
            result.setPropertyType(MatchState.EXACT);
        }

        // Furnished: only relevant when the viewer wants a furnished place.
        if (Boolean.TRUE.equals(prefs.getPreferredFurnished()) && Boolean.TRUE.equals(furnished)) {
            result.setFurnished(MatchState.EXACT);
        }

        // Pets allowed: only relevant when the viewer needs pets to be allowed.
        if (Boolean.TRUE.equals(prefs.getPreferredPetsAllowed()) && Boolean.TRUE.equals(petsAllowed)) {
            result.setPetsAllowed(MatchState.EXACT);
        }

        // Minimum bedrooms / bathrooms.
        if (prefs.getPreferredMinBedrooms() != null && bedrooms != null
                && bedrooms >= prefs.getPreferredMinBedrooms()) {
            result.setBedrooms(MatchState.EXACT);
        }
        if (prefs.getPreferredMinBathrooms() != null && bathrooms != null
                && bathrooms >= prefs.getPreferredMinBathrooms()) {
            result.setBathrooms(MatchState.EXACT);
        }

        // Location: only an exact city match is marked (country-only is not a full match).
        if ((prefs.getPreferredCity() != null || prefs.getPreferredCountry() != null)
                && (city != null || country != null)
                && ListingMatchSupport.scoreLocation(prefs.getPreferredCity(),
                        prefs.getPreferredCountry(), city, country) >= 1.0) {
            result.setLocation(MatchState.EXACT);
        }

        // Amenities: report only the preferred amenities the listing actually offers.
        result.setMatchedRoomAmenities(intersection(prefs.getPreferredRoomAmenities(), roomAmenities));
        result.setMatchedNearbyAmenities(intersection(prefs.getPreferredNearbyAmenities(), nearbyAmenities));

        return result;
    }

    private static <T> Set<T> intersection(Set<T> preferred, Set<T> available) {
        if (preferred == null || preferred.isEmpty() || available == null || available.isEmpty()) {
            return new LinkedHashSet<>();
        }
        return preferred.stream()
                .filter(available::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
