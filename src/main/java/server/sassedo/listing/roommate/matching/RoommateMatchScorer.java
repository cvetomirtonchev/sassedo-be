package server.sassedo.listing.roommate.matching;

import org.springframework.stereotype.Component;
import server.sassedo.listing.common.OccupationPreference;
import server.sassedo.listing.common.PetPolicy;
import server.sassedo.listing.common.SmokerPreference;
import server.sassedo.listing.common.matching.ListingMatchSupport;
import server.sassedo.listing.roommate.data.dto.RoommateListing;
import server.sassedo.user.data.dto.JobStatus;
import server.sassedo.user.data.dto.Language;
import server.sassedo.user.data.dto.Occupation;
import server.sassedo.user.data.dto.Sex;
import server.sassedo.user.data.dto.User;
import server.sassedo.user.data.dto.UserPreferencesDto;

import java.util.Set;

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
    private static final double WEIGHT_OCCUPATION = 15;
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
        if (user == null || listing == null) {
            return null;
        }

        double weightedSum = 0;
        double applicableWeight = 0;
        boolean anyConstraint = false;

        // Sex
        if (listing.getPreferredSex() != null) {
            anyConstraint = true;
            if (user.getSex() != null) {
                applicableWeight += WEIGHT_SEX;
                weightedSum += WEIGHT_SEX * scoreSex(user.getSex(), listing.getPreferredSex());
            }
        }

        // Age range
        if (listing.getAgeMin() != null || listing.getAgeMax() != null) {
            anyConstraint = true;
            if (user.getAge() != null) {
                applicableWeight += WEIGHT_AGE;
                weightedSum += WEIGHT_AGE * scoreAge(user.getAge(), listing.getAgeMin(), listing.getAgeMax());
            }
        }

        // Smoking preference (only a hard preference counts)
        if (listing.getSmokingPreference() != null
                && listing.getSmokingPreference() != SmokerPreference.NO_PREFERENCE) {
            anyConstraint = true;
            if (user.getSmokingPreference() != null) {
                applicableWeight += WEIGHT_SMOKING;
                weightedSum += WEIGHT_SMOKING
                        * scoreSmoking(user.getSmokingPreference(), listing.getSmokingPreference());
            }
        }

        // Occupation preference (only a hard preference counts)
        if (listing.getOccupationPreference() != null
                && listing.getOccupationPreference() != OccupationPreference.NO_PREFERENCE) {
            anyConstraint = true;
            Boolean userIsStudent = userIsStudent(user);
            if (userIsStudent != null) {
                applicableWeight += WEIGHT_OCCUPATION;
                weightedSum += WEIGHT_OCCUPATION
                        * scoreOccupation(userIsStudent, listing.getOccupationPreference());
            }
        }

        // Pet policy
        if (listing.getPetPolicy() != null) {
            anyConstraint = true;
            if (user.getPetPolicy() != null) {
                applicableWeight += WEIGHT_PETS;
                weightedSum += WEIGHT_PETS * scorePets(user.getPetPolicy(), listing.getPetPolicy());
            }
        }

        // Employment status
        if (listing.getEmploymentStatus() != null) {
            anyConstraint = true;
            if (user.getJobStatus() != null) {
                applicableWeight += WEIGHT_EMPLOYMENT;
                weightedSum += WEIGHT_EMPLOYMENT
                        * scoreEmployment(user.getJobStatus(), listing.getEmploymentStatus());
            }
        }

        // Languages
        if (listing.getSpokenLanguages() != null && !listing.getSpokenLanguages().isEmpty()) {
            anyConstraint = true;
            if (user.getLanguages() != null && !user.getLanguages().isEmpty()) {
                applicableWeight += WEIGHT_LANGUAGES;
                weightedSum += WEIGHT_LANGUAGES
                        * scoreLanguages(user.getLanguages(), listing.getSpokenLanguages());
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
            return 100;
        }
        if (applicableWeight == 0) {
            return null;
        }
        return (int) Math.round(100.0 * weightedSum / applicableWeight);
    }

    private double scoreSex(Sex userSex, Sex preferredSex) {
        return userSex == preferredSex ? 1.0 : 0.0;
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

    /**
     * @return {@code true} if the user is a student, {@code false} if working, or {@code null} when
     * the user's occupation/job status does not map cleanly to either bucket.
     */
    private Boolean userIsStudent(User user) {
        Occupation occupation = user.getOccupation();
        if (occupation == Occupation.STUDENT) {
            return true;
        }
        if (occupation == Occupation.WORKING
                || occupation == Occupation.SELF_EMPLOYED
                || occupation == Occupation.REMOTE_WORKER) {
            return false;
        }
        JobStatus jobStatus = user.getJobStatus();
        if (jobStatus == JobStatus.STUDENT) {
            return true;
        }
        if (jobStatus == JobStatus.EMPLOYED || jobStatus == JobStatus.SELF_EMPLOYED) {
            return false;
        }
        return null;
    }

    private double scoreOccupation(boolean userIsStudent, OccupationPreference required) {
        return switch (required) {
            case STUDENT -> userIsStudent ? 1.0 : 0.0;
            case WORKING -> userIsStudent ? 0.0 : 1.0;
            case NO_PREFERENCE -> 1.0;
        };
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

    private double scoreEmployment(JobStatus user, JobStatus required) {
        if (user == required) {
            return 1.0;
        }
        boolean userWorks = user == JobStatus.EMPLOYED || user == JobStatus.SELF_EMPLOYED;
        boolean requiredWorks = required == JobStatus.EMPLOYED || required == JobStatus.SELF_EMPLOYED;
        if (userWorks && requiredWorks) {
            return 0.6;
        }
        return 0.0;
    }

    private double scoreLanguages(Set<Language> userLanguages, Set<Language> requiredLanguages) {
        long overlap = requiredLanguages.stream().filter(userLanguages::contains).count();
        return (double) overlap / requiredLanguages.size();
    }
}
