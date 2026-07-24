package server.sassedo.user.service.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.sassedo.listing.common.ListingStatus;
import server.sassedo.listing.rental.repository.RentalListingRepository;
import server.sassedo.listing.roommate.repository.RoommateListingRepository;
import server.sassedo.model.GenericException;
import server.sassedo.model.GenericExceptionCode;
import server.sassedo.promotion.service.PromotionService;
import server.sassedo.user.data.dto.User;
import server.sassedo.user.data.dto.UserPreferencesDto;
import server.sassedo.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountDeletionService {

    private static final List<ListingStatus> UNPUBLISHED_STATUSES = List.of(
            ListingStatus.PENDING, ListingStatus.ACTIVE, ListingStatus.INACTIVE);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RentalListingRepository rentalListingRepository;
    private final RoommateListingRepository roommateListingRepository;
    private final PromotionService promotionService;

    @Transactional(rollbackFor = GenericException.class)
    public void deleteOwnAccount(Long userId, String currentPassword) throws GenericException {
        User user = requireActiveUserForUpdate(userId);
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new GenericException(GenericExceptionCode.INCORRECT_PASSWORD,
                    "Current password is incorrect");
        }
        deleteAccount(user);
    }

    @Transactional(rollbackFor = GenericException.class)
    public void deleteByAdmin(Long userId) throws GenericException {
        deleteAccount(requireActiveUserForUpdate(userId));
    }

    private User requireActiveUserForUpdate(Long userId) throws GenericException {
        return userRepository.findActiveByIdForUpdate(userId)
                .orElseThrow(() -> new GenericException(GenericExceptionCode.USER_NOT_FOUND,
                        "User not found"));
    }

    private void deleteAccount(User user) {
        LocalDateTime deletedAt = LocalDateTime.now();

        // Promotions are terminated first so any live listing tier is cleared before the listing
        // rows are expired. Purchases, payments, promotions and listing content remain intact.
        promotionService.cancelAllForOwner(user.getId());
        rentalListingRepository.expireByOwnerId(user.getId(), UNPUBLISHED_STATUSES, deletedAt);
        roommateListingRepository.expireByOwnerId(user.getId(), UNPUBLISHED_STATUSES, deletedAt);

        anonymize(user, deletedAt);
        userRepository.save(user);
    }

    private void anonymize(User user, LocalDateTime deletedAt) {
        user.setEmail("deleted-" + user.getId() + "@sassedo.invalid");
        user.setPassword(passwordEncoder.encode(UUID.randomUUID() + UUID.randomUUID().toString()));
        user.setName("Deleted account");
        user.setFirstName(null);
        user.setLastName(null);
        user.setPhone(null);
        user.setProfilePhoto(null);
        user.setAge(null);
        user.setSex(null);
        if (user.getLanguages() != null) {
            user.getLanguages().clear();
        }
        user.setProfession(null);
        user.setSmokingPreference(null);
        user.setPetPolicy(null);
        user.setOccupation(null);
        user.setShortDescription(null);
        clearPreferences(user.getPreferences());
        user.setVerificationCode(null);
        user.setEnabled(false);
        user.setBlocked(true);
        if (user.getRoles() != null) {
            user.getRoles().clear();
        }
        if (user.getPasswordResetTokens() != null) {
            user.getPasswordResetTokens().clear();
        }
        user.setTermsAndConditionsAccepted(false);
        user.setGdprAccepted(false);
        user.setTermsAndConditionsAcceptedAt(null);
        user.setGdprAcceptedAt(null);
        user.setMarketingConsentAccepted(false);
        user.setMarketingConsentAcceptedAt(null);
        user.setMarketingConsentUpdatedAt(null);
        user.setDeletedAt(deletedAt);
    }

    private void clearPreferences(UserPreferencesDto preferences) {
        if (preferences == null) {
            return;
        }
        preferences.setPreferredMaxBudget(null);
        preferences.setPreferredPropertyType(null);
        preferences.setPreferredFurnished(null);
        preferences.setPreferredPetsAllowed(null);
        preferences.setPreferredMinBedrooms(null);
        preferences.setPreferredMinBathrooms(null);
        preferences.setPreferredCountry(null);
        preferences.setPreferredCity(null);
        if (preferences.getPreferredRoomAmenities() != null) {
            preferences.getPreferredRoomAmenities().clear();
        }
        if (preferences.getPreferredNearbyAmenities() != null) {
            preferences.getPreferredNearbyAmenities().clear();
        }
    }
}
