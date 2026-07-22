package server.sassedo;

import org.junit.jupiter.api.Test;
import server.sassedo.listing.common.PetPolicy;
import server.sassedo.listing.common.SmokerPreference;
import server.sassedo.user.data.dto.Language;
import server.sassedo.user.data.dto.Occupation;
import server.sassedo.user.data.dto.Sex;
import server.sassedo.user.data.dto.User;
import server.sassedo.user.service.user.UserServiceImpl;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

class UserProfileCompletenessTest {

    private final UserServiceImpl userService = mock(UserServiceImpl.class, CALLS_REAL_METHODS);

    @Test
    void occupationIsRequiredForProfileCompleteness() {
        User user = completeUser();
        assertThat(userService.isProfileComplete(user)).isTrue();

        user.setOccupation(null);
        assertThat(userService.isProfileComplete(user)).isFalse();
    }

    private User completeUser() {
        User user = new User();
        user.setProfilePhoto(new byte[]{1});
        user.setFirstName("Test");
        user.setLastName("Person");
        user.setPhone("+359881234567");
        user.setAge(30);
        user.setSex(Sex.OTHER);
        user.setLanguages(Set.of(Language.ENGLISH));
        user.setOccupation(Occupation.PREFER_NOT_TO_SAY);
        user.setSmokingPreference(SmokerPreference.NO_PREFERENCE);
        user.setPetPolicy(PetPolicy.NOT_ALLOWED);
        return user;
    }
}
