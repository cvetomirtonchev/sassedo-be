package server.sassedo.listing.roommate.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.sassedo.listing.common.PetPolicy;
import server.sassedo.listing.common.SmokerPreference;
import server.sassedo.listing.roommate.data.dto.RoommateListing;
import server.sassedo.listing.roommate.data.network.response.OwnerProfileResponse;
import server.sassedo.listing.roommate.data.network.response.RoommateListingResponse;
import server.sassedo.listing.roommate.matching.RoommateMatchScorer;
import server.sassedo.listing.roommate.repository.RoommateListingPhotoRepository;
import server.sassedo.user.data.dto.Language;
import server.sassedo.user.data.dto.Occupation;
import server.sassedo.user.data.dto.Sex;
import server.sassedo.user.data.projection.PublicProfileView;
import server.sassedo.user.service.user.UserService;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoommateListingMapperTest {

    @Mock
    private UserService userService;
    @Mock
    private RoommateMatchScorer matchScorer;
    @Mock
    private RoommateListingPhotoRepository photoRepository;

    private RoommateListingMapper mapper() {
        return new RoommateListingMapper(userService, matchScorer, photoRepository);
    }

    private PublicProfileView profile() {
        PublicProfileView p = mock(PublicProfileView.class);
        when(p.getAge()).thenReturn(28);
        when(p.getSex()).thenReturn(Sex.MALE);
        when(p.getPetPolicy()).thenReturn(PetPolicy.CATS);
        when(p.getSmokingPreference()).thenReturn(SmokerPreference.NON_SMOKER);
        when(p.getOccupation()).thenReturn(Occupation.REMOTE_WORKER);
        when(p.getShortDescription()).thenReturn("Friendly and tidy.");
        when(p.getHasPhoto()).thenReturn(false);
        return p;
    }

    @Test
    void mapPreservesTotalPeopleInProperty() {
        RoommateListing listing = new RoommateListing();
        listing.setId(10L);
        listing.setPeopleInProperty(2);
        when(photoRepository.findMetaByListingId(10L)).thenReturn(List.of());

        RoommateListingResponse response = mapper().map(listing);

        assertThat(response.getPeopleInProperty()).isEqualTo(2);
    }

    @Test
    void enrichOwnerProfilePopulatesSafeProfileSubset() {
        PublicProfileView profile = profile();
        when(userService.getPublicProfile(2L)).thenReturn(profile);
        when(userService.getUserLanguages(2L)).thenReturn(Set.of(Language.ENGLISH, Language.BULGARIAN));

        RoommateListingResponse response = new RoommateListingResponse();
        response.setOwnerName("Bob");

        mapper().enrichOwnerProfile(response, 2L);

        OwnerProfileResponse ownerProfile = response.getOwnerProfile();
        assertThat(ownerProfile).isNotNull();
        assertThat(ownerProfile.getAge()).isEqualTo(28);
        assertThat(ownerProfile.getSex()).isEqualTo(Sex.MALE);
        assertThat(ownerProfile.getPetPolicy()).isEqualTo(PetPolicy.CATS);
        assertThat(ownerProfile.getSmokingPreference()).isEqualTo(SmokerPreference.NON_SMOKER);
        assertThat(ownerProfile.getEmploymentStatus()).isEqualTo(Occupation.REMOTE_WORKER);
        assertThat(ownerProfile.getAboutMe()).isEqualTo("Friendly and tidy.");
        assertThat(ownerProfile.getLanguages()).containsExactlyInAnyOrder(Language.ENGLISH, Language.BULGARIAN);
    }

    @Test
    void enrichOwnerProfileLeavesProfileNullWhenOwnerMissing() {
        when(userService.getPublicProfile(9L)).thenReturn(null);

        RoommateListingResponse response = new RoommateListingResponse();
        mapper().enrichOwnerProfile(response, 9L);

        assertThat(response.getOwnerProfile()).isNull();
    }

    @Test
    void enrichOwnerProfileIgnoresNullOwnerId() {
        RoommateListingResponse response = new RoommateListingResponse();
        mapper().enrichOwnerProfile(response, null);

        assertThat(response.getOwnerProfile()).isNull();
    }

    @Test
    void ownerProfileResponseNeverExposesPrivateFields() {
        List<String> fieldNames = Arrays.stream(OwnerProfileResponse.class.getDeclaredFields())
                .map(Field::getName)
                .toList();

        assertThat(fieldNames)
                .doesNotContain("email", "phone", "password", "roles", "preferences", "profilePhoto");
    }
}
