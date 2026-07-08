package server.sassedo.listing.rental.matching;

import org.springframework.stereotype.Component;
import server.sassedo.listing.common.PetPolicy;
import server.sassedo.listing.common.matching.ListingMatchSupport;
import server.sassedo.listing.rental.data.dto.RentalListing;
import server.sassedo.user.data.dto.User;
import server.sassedo.user.data.dto.UserPreferencesDto;

/**
 * Scores how well a rental listing matches a user's property preferences. Only the preference
 * dimensions that map onto {@link RentalListing} are used: budget, property type, pets (via
 * {@code petPolicy}), min bedrooms, min bathrooms, nearby amenities and location. Furnished and
 * room-amenity preferences do not apply to rentals.
 *
 * <p>Returns {@code null} when the user has no preferences set (so no match badge is shown until at
 * least one preference exists) or when a preference exists but nothing on the listing is scorable;
 * otherwise the weighted average over applicable dimensions scaled to 0-100.
 */
@Component
public class RentalMatchScorer {

    private static final double WEIGHT_BUDGET = 15;
    private static final double WEIGHT_PROPERTY_TYPE = 10;
    private static final double WEIGHT_PETS_ALLOWED = 8;
    private static final double WEIGHT_MIN_BEDROOMS = 8;
    private static final double WEIGHT_MIN_BATHROOMS = 8;
    private static final double WEIGHT_NEARBY_AMENITIES = 10;
    private static final double WEIGHT_LOCATION = 15;

    public Integer score(User user, RentalListing listing) {
        if (user == null || listing == null) {
            return null;
        }
        UserPreferencesDto prefs = user.getPreferences();
        if (prefs == null) {
            return null;
        }

        double weightedSum = 0;
        double applicableWeight = 0;
        boolean anyConstraint = false;

        // Max budget
        if (prefs.getPreferredMaxBudget() != null) {
            anyConstraint = true;
            if (listing.getRent() != null) {
                applicableWeight += WEIGHT_BUDGET;
                weightedSum += WEIGHT_BUDGET
                        * ListingMatchSupport.scoreBudget(listing.getRent(), prefs.getPreferredMaxBudget());
            }
        }

        // Property type
        if (prefs.getPreferredPropertyType() != null) {
            anyConstraint = true;
            if (listing.getPropertyType() != null) {
                applicableWeight += WEIGHT_PROPERTY_TYPE;
                weightedSum += WEIGHT_PROPERTY_TYPE
                        * (listing.getPropertyType() == prefs.getPreferredPropertyType() ? 1.0 : 0.0);
            }
        }

        // Pets allowed (only when the user needs pets to be allowed)
        if (Boolean.TRUE.equals(prefs.getPreferredPetsAllowed())) {
            anyConstraint = true;
            if (listing.getPetPolicy() != null) {
                applicableWeight += WEIGHT_PETS_ALLOWED;
                weightedSum += WEIGHT_PETS_ALLOWED
                        * (listing.getPetPolicy() != PetPolicy.NOT_ALLOWED ? 1.0 : 0.0);
            }
        }

        // Minimum bedrooms
        if (prefs.getPreferredMinBedrooms() != null) {
            anyConstraint = true;
            if (listing.getBedrooms() != null) {
                applicableWeight += WEIGHT_MIN_BEDROOMS;
                weightedSum += WEIGHT_MIN_BEDROOMS
                        * (listing.getBedrooms() >= prefs.getPreferredMinBedrooms() ? 1.0 : 0.0);
            }
        }

        // Minimum bathrooms
        if (prefs.getPreferredMinBathrooms() != null) {
            anyConstraint = true;
            if (listing.getBathrooms() != null) {
                applicableWeight += WEIGHT_MIN_BATHROOMS;
                weightedSum += WEIGHT_MIN_BATHROOMS
                        * (listing.getBathrooms() >= prefs.getPreferredMinBathrooms() ? 1.0 : 0.0);
            }
        }

        // Preferred nearby amenities
        if (prefs.getPreferredNearbyAmenities() != null && !prefs.getPreferredNearbyAmenities().isEmpty()) {
            anyConstraint = true;
            if (listing.getNearbyAmenities() != null && !listing.getNearbyAmenities().isEmpty()) {
                applicableWeight += WEIGHT_NEARBY_AMENITIES;
                weightedSum += WEIGHT_NEARBY_AMENITIES
                        * ListingMatchSupport.amenityOverlap(prefs.getPreferredNearbyAmenities(),
                        listing.getNearbyAmenities());
            }
        }

        // Location (preferred city / country)
        if (prefs.getPreferredCity() != null || prefs.getPreferredCountry() != null) {
            anyConstraint = true;
            if (listing.getCity() != null || listing.getCountry() != null) {
                applicableWeight += WEIGHT_LOCATION;
                weightedSum += WEIGHT_LOCATION
                        * ListingMatchSupport.scoreLocation(prefs.getPreferredCity(),
                        prefs.getPreferredCountry(), listing.getCity(), listing.getCountry());
            }
        }

        if (!anyConstraint) {
            return null;
        }
        if (applicableWeight == 0) {
            return null;
        }
        return (int) Math.round(100.0 * weightedSum / applicableWeight);
    }
}
