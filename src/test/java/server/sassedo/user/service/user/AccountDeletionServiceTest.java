package server.sassedo.user.service.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import server.sassedo.listing.common.ListingStatus;
import server.sassedo.listing.common.NearbyAmenity;
import server.sassedo.listing.common.RoomAmenity;
import server.sassedo.listing.rental.repository.RentalListingRepository;
import server.sassedo.listing.roommate.repository.RoommateListingRepository;
import server.sassedo.model.GenericException;
import server.sassedo.promotion.service.PromotionService;
import server.sassedo.user.data.dto.ERole;
import server.sassedo.user.data.dto.Language;
import server.sassedo.user.data.dto.PasswordResetToken;
import server.sassedo.user.data.dto.Role;
import server.sassedo.user.data.dto.User;
import server.sassedo.user.data.dto.UserPreferencesDto;
import server.sassedo.user.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountDeletionServiceTest {

    private static final List<ListingStatus> UNPUBLISHED_STATUSES = List.of(
            ListingStatus.PENDING, ListingStatus.ACTIVE, ListingStatus.INACTIVE);

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RentalListingRepository rentalListingRepository;
    @Mock
    private RoommateListingRepository roommateListingRepository;
    @Mock
    private PromotionService promotionService;

    @InjectMocks
    private AccountDeletionService service;

    @Test
    void deleteOwnAccount_rejectsAnIncorrectPasswordWithoutChangingData() {
        User user = user();
        when(userRepository.findActiveByIdForUpdate(42L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "password-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.deleteOwnAccount(42L, "wrong"))
                .isInstanceOf(GenericException.class)
                .hasMessage("Current password is incorrect");

        verifyNoInteractions(promotionService, rentalListingRepository, roommateListingRepository);
        verify(userRepository, never()).save(any());
        verify(userRepository, never()).delete(any());
    }

    @Test
    void deleteOwnAccount_anonymizesUserAndRetainsOwnedHistory() throws GenericException {
        User user = user();
        when(userRepository.findActiveByIdForUpdate(42L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct", "password-hash")).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("redacted-password");

        service.deleteOwnAccount(42L, "correct");

        verify(promotionService).cancelAllForOwner(42L);
        verify(rentalListingRepository).expireByOwnerId(
                eq(42L), eq(UNPUBLISHED_STATUSES), any(LocalDateTime.class));
        verify(roommateListingRepository).expireByOwnerId(
                eq(42L), eq(UNPUBLISHED_STATUSES), any(LocalDateTime.class));
        verify(userRepository).save(user);
        verify(userRepository, never()).delete(any());

        assertThat(user.getId()).isEqualTo(42L);
        assertThat(user.getEmail()).isEqualTo("deleted-42@sassedo.invalid");
        assertThat(user.getPassword()).isEqualTo("redacted-password");
        assertThat(user.getName()).isEqualTo("Deleted account");
        assertThat(user.getFirstName()).isNull();
        assertThat(user.getLastName()).isNull();
        assertThat(user.getPhone()).isNull();
        assertThat(user.getProfilePhoto()).isNull();
        assertThat(user.getLanguages()).isEmpty();
        assertThat(user.getRoles()).isEmpty();
        assertThat(user.getPasswordResetTokens()).isEmpty();
        assertThat(user.isEnabled()).isFalse();
        assertThat(user.isBlocked()).isTrue();
        assertThat(user.isTermsAndConditionsAccepted()).isFalse();
        assertThat(user.isGdprAccepted()).isFalse();
        assertThat(user.isMarketingConsentAccepted()).isFalse();
        assertThat(user.getTermsAndConditionsAcceptedAt()).isNull();
        assertThat(user.getGdprAcceptedAt()).isNull();
        assertThat(user.getMarketingConsentAcceptedAt()).isNull();
        assertThat(user.getMarketingConsentUpdatedAt()).isNull();
        assertThat(user.getDeletedAt()).isNotNull();

        UserPreferencesDto preferences = user.getPreferences();
        assertThat(preferences.getPreferredMaxBudget()).isNull();
        assertThat(preferences.getPreferredRoomAmenities()).isEmpty();
        assertThat(preferences.getPreferredNearbyAmenities()).isEmpty();
    }

    @Test
    void deleteByAdmin_usesTheSameRetentionWorkflowWithoutCheckingPassword() throws GenericException {
        User user = user();
        when(userRepository.findActiveByIdForUpdate(42L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn("redacted-password");

        service.deleteByAdmin(42L);

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(promotionService).cancelAllForOwner(42L);
        verify(userRepository).save(user);
        assertThat(user.getDeletedAt()).isNotNull();
    }

    private User user() {
        User user = new User();
        user.setId(42L);
        user.setEmail("person@example.com");
        user.setPassword("password-hash");
        user.setName("Person Example");
        user.setFirstName("Person");
        user.setLastName("Example");
        user.setPhone("+359123456789");
        user.setProfilePhoto(new byte[]{1, 2, 3});
        user.setLanguages(new java.util.LinkedHashSet<>(List.of(Language.ENGLISH)));
        user.getRoles().add(new Role(ERole.ROLE_USER));
        user.getPasswordResetTokens().add(
                new PasswordResetToken("123456", user, new Date()));
        user.setEnabled(true);
        user.setTermsAndConditionsAccepted(true);
        user.setGdprAccepted(true);
        user.setTermsAndConditionsAcceptedAt(LocalDateTime.now());
        user.setGdprAcceptedAt(LocalDateTime.now());
        user.setMarketingConsentAccepted(true);
        user.setMarketingConsentAcceptedAt(LocalDateTime.now());
        user.setMarketingConsentUpdatedAt(LocalDateTime.now());

        UserPreferencesDto preferences = new UserPreferencesDto();
        preferences.setPreferredMaxBudget(BigDecimal.valueOf(1000));
        preferences.getPreferredRoomAmenities().add(RoomAmenity.BALCONY);
        preferences.getPreferredNearbyAmenities().add(NearbyAmenity.PUBLIC_TRANSPORT);
        user.setPreferences(preferences);
        return user;
    }
}
