package server.sassedo.listing.roommate.matching;

import org.springframework.stereotype.Component;
import server.sassedo.listing.common.PetPolicy;
import server.sassedo.listing.common.RoommateSexPreference;
import server.sassedo.listing.common.SmokerPreference;
import server.sassedo.listing.common.matching.ListingMatchSupport;
import server.sassedo.listing.roommate.data.dto.RoommateListing;
import server.sassedo.user.data.dto.Language;
import server.sassedo.user.data.dto.Occupation;
import server.sassedo.user.data.dto.Sex;
import server.sassedo.user.data.dto.User;
import server.sassedo.user.data.dto.UserPreferencesDto;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Scores how well a user matches a roommate listing, combining two families of dimensions:
 * <ul>
 *     <li><b>Person vs requirements</b> - the user's profile against the listing's "Step 4"
 *     roommate requirements (sex, age, smoking, ...).</li>
 *     <li><b>Preferences vs property details</b> - the user's optional property preferences against
 *     the listing's "Step 3" property details (budget, property type, amenities, location, ...).</li>
 * </ul>
 *
 * <p>Each dimension produces a score in [0,1] and only contributes when BOTH sides have the
 * relevant data (i.e. the listing specifies a requirement / has the property value AND the user has
 * the corresponding profile data / preference). The final score is the weighted average over the
 * applicable dimensions, scaled to 0-100.
 *
 * <ul>
 *     <li>If neither a listing requirement nor a user preference applies at all, the score is 100
 *     (nothing to fail).</li>
 *     <li>If constraints exist but the user has no usable data for any of them, the result is
 *     {@code null} so the frontend can prompt the user to complete their profile.</li>
 * </ul>
 */
@Component
public class RoommateMatchScorer {

    // Weights are relative; the result is normalized over whichever dimensions apply.
    private static final double WEIGHT_SEX = 20;
    private static final double WEIGHT_AGE = 20;
    private static final double WEIGHT_SMOKING = 15;
    private static final double WEIGHT_PETS = 10;
    private static final double WEIGHT_EMPLOYMENT = 10;
    private static final double WEIGHT_LANGUAGES = 10;

    // Preference (property-detail) weights.
    private static final double WEIGHT_BUDGET = 15;
    private static final double WEIGHT_PROPERTY_TYPE = 10;
    private static final double WEIGHT_FURNISHED = 8;
    private static final double WEIGHT_PETS_ALLOWED = 8;
    private static final double WEIGHT_MIN_BEDROOMS = 8;
    private static final double WEIGHT_MIN_BATHROOMS = 8;
    private static final double WEIGHT_LOCATION = 15;
    private static final double WEIGHT_ROOM_AMENITIES = 10;
    private static final double WEIGHT_NEARBY_AMENITIES = 10;

    // How many years outside the [min,max] range before an age scores 0.
    private static final double AGE_FALLOFF_YEARS = 3.0;

    /**
     * @return a 0-100 compatibility percentage, or {@code null} when no dimension could be scored
     * (e.g. an empty profile against a listing that does specify requirements).
     */
    public Integer score(User user, RoommateListing listing) {
        return evaluate(user, listing).getScore();
    }

    /**
     * Scores the user against the listing and, in addition to the aggregate {@link #score}, reports a
     * per-requirement {@link RequirementMatchState} so the frontend can highlight individual matches.
     * When {@code user} or {@code listing} is {@code null} the result has a {@code null} score and all
     * field states default to {@link RequirementMatchState#NONE}.
     */
    public RoommateMatchResult evaluate(User user, RoommateListing listing) {
        RoommateMatchResult.Builder result = RoommateMatchResult.builder();
        if (user == null || listing == null) {
            return result.build();
        }

        double weightedSum = 0;
        double applicableWeight = 0;
        boolean anyConstraint = false;

        // Sex
        // An explicit no-preference choice is visible as a match, but stays out of the weighted score.
        if (listing.getPreferredSex() == RoommateSexPreference.NO_PREFERENCE) {
            if (user.getSex() != null) {
                result.sex(RequirementMatchState.EXACT);
            }
        } else if (listing.getPreferredSex() != null) {
            anyConstraint = true;
            if (user.getSex() != null) {
                double s = scoreSex(user.getSex(), listing.getPreferredSex());
                applicableWeight += WEIGHT_SEX;
                weightedSum += WEIGHT_SEX * s;
                result.sex(stateOf(s));
            }
        }

        // Age range
        if (listing.getAgeMin() != null || listing.getAgeMax() != null) {
            anyConstraint = true;
            if (user.getAge() != null) {
                double s = scoreAge(user.getAge(), listing.getAgeMin(), listing.getAgeMax());
                applicableWeight += WEIGHT_AGE;
                weightedSum += WEIGHT_AGE * s;
                result.age(stateOf(s));
            }
        }

        // Smoking preference (only a hard preference counts)
        if (listing.getSmokingPreference() != null
                && listing.getSmokingPreference() != SmokerPreference.NO_PREFERENCE) {
            anyConstraint = true;
            if (user.getSmokingPreference() != null) {
                double s = scoreSmoking(user.getSmokingPreference(), listing.getSmokingPreference());
                applicableWeight += WEIGHT_SMOKING;
                weightedSum += WEIGHT_SMOKING * s;
                result.smoking(stateOf(s));
            }
        }

        // Pet policy
        if (listing.getPetPolicy() != null) {
            anyConstraint = true;
            if (user.getPetPolicy() != null) {
                double s = scorePets(user.getPetPolicy(), listing.getPetPolicy());
                applicableWeight += WEIGHT_PETS;
                weightedSum += WEIGHT_PETS * s;
                result.pets(stateOf(s));
            }
        }

        // Employment status
        if (listing.getEmploymentStatus() != null) {
            anyConstraint = true;
            if (user.getOccupation() != null) {
                double s = scoreEmployment(user.getOccupation(), listing.getEmploymentStatus());
                applicableWeight += WEIGHT_EMPLOYMENT;
                weightedSum += WEIGHT_EMPLOYMENT * s;
                result.employment(stateOf(s));
            }
        }

        // Languages
        if (listing.getSpokenLanguages() != null && !listing.getSpokenLanguages().isEmpty()) {
            anyConstraint = true;
            if (user.getLanguages() != null && !user.getLanguages().isEmpty()) {
                Set<Language> matchedLanguages = listing.getSpokenLanguages().stream()
                        .filter(user.getLanguages()::contains)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                double s = scoreLanguages(matchedLanguages);
                applicableWeight += WEIGHT_LANGUAGES;
                weightedSum += WEIGHT_LANGUAGES * s;
                result.languages(stateOf(s));
                result.matchedLanguages(matchedLanguages);
            }
        }

        // --- Preferences vs property details (each applies only when the user set the preference) ---
        UserPreferencesDto prefs = user.getPreferences();
        if (prefs != null) {
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

            // Furnished (only when the user wants a furnished place)
            if (Boolean.TRUE.equals(prefs.getPreferredFurnished())) {
                anyConstraint = true;
                if (listing.getFurnished() != null) {
                    applicableWeight += WEIGHT_FURNISHED;
                    weightedSum += WEIGHT_FURNISHED * (Boolean.TRUE.equals(listing.getFurnished()) ? 1.0 : 0.0);
                }
            }

            // Pets allowed (only when the user needs pets to be allowed)
            if (Boolean.TRUE.equals(prefs.getPreferredPetsAllowed())) {
                anyConstraint = true;
                if (listing.getPetsAllowed() != null) {
                    applicableWeight += WEIGHT_PETS_ALLOWED;
                    weightedSum += WEIGHT_PETS_ALLOWED * (Boolean.TRUE.equals(listing.getPetsAllowed()) ? 1.0 : 0.0);
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

            // Preferred room amenities
            if (prefs.getPreferredRoomAmenities() != null && !prefs.getPreferredRoomAmenities().isEmpty()) {
                anyConstraint = true;
                if (listing.getRoomAmenities() != null && !listing.getRoomAmenities().isEmpty()) {
                    applicableWeight += WEIGHT_ROOM_AMENITIES;
                    weightedSum += WEIGHT_ROOM_AMENITIES
                            * ListingMatchSupport.amenityOverlap(prefs.getPreferredRoomAmenities(),
                            listing.getRoomAmenities());
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
        }

        if (!anyConstraint) {
            return result.score(100).build();
        }
        if (applicableWeight == 0) {
            return result.score(null).build();
        }
        return result.score((int) Math.round(100.0 * weightedSum / applicableWeight)).build();
    }

    private RequirementMatchState stateOf(double dimensionScore) {
        if (dimensionScore >= 1.0) {
            return RequirementMatchState.EXACT;
        }
        if (dimensionScore > 0.0) {
            return RequirementMatchState.PARTIAL;
        }
        return RequirementMatchState.NONE;
    }

    private double scoreSex(Sex userSex, RoommateSexPreference preferredSex) {
        boolean matches = (userSex == Sex.MALE && preferredSex == RoommateSexPreference.MALE)
                || (userSex == Sex.FEMALE && preferredSex == RoommateSexPreference.FEMALE);
        return matches ? 1.0 : 0.0;
    }

    private double scoreAge(int age, Integer min, Integer max) {
        int lo = min != null ? min : Integer.MIN_VALUE;
        int hi = max != null ? max : Integer.MAX_VALUE;
        if (age >= lo && age <= hi) {
            return 1.0;
        }
        int distance = age < lo ? (lo - age) : (age - hi);
        double falloff = 1.0 - (distance / AGE_FALLOFF_YEARS);
        return Math.max(0.0, falloff);
    }

    private double scoreSmoking(SmokerPreference user, SmokerPreference required) {
        if (user == required) {
            return 1.0;
        }
        // A user with no strong preference is a soft match for either requirement.
        if (user == SmokerPreference.NO_PREFERENCE) {
            return 0.7;
        }
        return 0.0;
    }

    private double scorePets(PetPolicy user, PetPolicy required) {
        if (required == PetPolicy.ALL_ALLOWED) {
            return 1.0;
        }
        if (required == PetPolicy.NOT_ALLOWED) {
            return user == PetPolicy.NOT_ALLOWED ? 1.0 : 0.0;
        }
        // required is DOGS or CATS: exact match or a user who keeps no pets is fine.
        if (user == required || user == PetPolicy.NOT_ALLOWED) {
            return 1.0;
        }
        if (user == PetPolicy.ALL_ALLOWED) {
            return 0.7;
        }
        return 0.0;
    }

    private double scoreEmployment(Occupation user, Occupation required) {
        // Employment is a strict match: either the viewer's status equals the requirement or it does
        // not count at all (no soft match between employed and self-employed).
        return user == required ? 1.0 : 0.0;
    }

    /**
     * Any overlap between the viewer's languages and the required languages counts as a full match.
     */
    private double scoreLanguages(Set<Language> matchedLanguages) {
        return matchedLanguages.isEmpty() ? 0.0 : 1.0;
    }
}
