package server.sassedo.listing.roommate.repository;

import org.junit.jupiter.api.Test;
import server.sassedo.listing.common.RoommateSexPreference;
import server.sassedo.user.data.dto.Sex;

import static org.assertj.core.api.Assertions.assertThat;

class RoommateListingSpecificationsTest {

    @Test
    void maleAndFemaleFiltersAcceptTheExplicitPreferenceAndNoPreference() {
        assertThat(RoommateListingSpecifications.acceptedSexPreferences(Sex.MALE))
                .containsExactly(RoommateSexPreference.MALE, RoommateSexPreference.NO_PREFERENCE);
        assertThat(RoommateListingSpecifications.acceptedSexPreferences(Sex.FEMALE))
                .containsExactly(RoommateSexPreference.FEMALE, RoommateSexPreference.NO_PREFERENCE);
    }

    @Test
    void otherProfileSexIsAcceptedByNoPreferenceListings() {
        assertThat(RoommateListingSpecifications.acceptedSexPreferences(Sex.OTHER))
                .containsExactly(RoommateSexPreference.NO_PREFERENCE);
    }
}
