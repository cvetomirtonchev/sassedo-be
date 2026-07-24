package server.sassedo.user.service.user;

import lombok.RequiredArgsConstructor;
import java.security.SecureRandom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import server.sassedo.location.data.dto.City;
import server.sassedo.location.data.dto.Country;
import server.sassedo.location.repository.CityRepository;
import server.sassedo.location.repository.CountryRepository;
import server.sassedo.model.GenericException;
import server.sassedo.model.GenericExceptionCode;
import server.sassedo.utils.ImageProcessor;
import server.sassedo.utils.ImageUploadValidator;
import server.sassedo.user.data.dto.ERole;
import server.sassedo.user.data.dto.Language;
import server.sassedo.user.data.dto.PasswordResetToken;
import server.sassedo.user.data.dto.Role;
import server.sassedo.user.data.dto.User;
import server.sassedo.user.data.dto.UserPreferencesDto;
import server.sassedo.user.data.projection.PublicProfileView;
import server.sassedo.user.data.projection.UserParticipantSummary;
import server.sassedo.user.data.network.UpdateUserRequest;
import server.sassedo.user.data.network.request.*;
import server.sassedo.user.repository.PasswordTokenRepository;
import server.sassedo.user.repository.RoleRepository;
import server.sassedo.user.repository.UserRepository;
import server.sassedo.user.service.EmailVerificationService;
import server.sassedo.utils.PasswordValidator;

import jakarta.mail.MessagingException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder encoder;
    private final PasswordTokenRepository passwordTokenRepository;
    private final EmailVerificationService emailVerificationService;
    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;

    @Override
    @Transactional
    public void registerUser(RegisterRequest signUpRequest, String siteURL) throws GenericException {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new GenericException(GenericExceptionCode.EMAIL_ALREADY_EXISTS, "Error: Email is already in use!");
        }

        if (!PasswordValidator.isPasswordStrong(signUpRequest.getPassword())) {
            throw new GenericException(GenericExceptionCode.PASSWORD_NOT_STRONG, "Error: Password is not strong enough!");
        }

        if (!signUpRequest.isAcceptedTerms() || !signUpRequest.isAcceptedGdpr()) {
            throw new GenericException(GenericExceptionCode.CONSENT_REQUIRED, "Error: Terms and privacy policy must be accepted!");
        }

        // Create new user's account
        User user = new User(
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()),
                signUpRequest.getFirstName(),
                signUpRequest.getLastName(),
                signUpRequest.getPhone(),
                generateRandomString(64),
                signUpRequest.isAcceptedTerms(),
                signUpRequest.isAcceptedGdpr()
        );

        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName(ERole.ROLE_USER).orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        roles.add(userRole);
        user.setRoles(roles);

        userRepository.save(user);
    }

    @Override
    public void verify(VerifyUserRequest verifyUserRequest) throws GenericException {
        User user = userRepository.findByEmail(verifyUserRequest.getEmail());
        if (user == null) {
            throw new GenericException(GenericExceptionCode.USER_NOT_FOUND, "User not found");
        }
        if (user.isEnabled()) {
            throw new GenericException(GenericExceptionCode.USER_ALREADY_VERIFIED, "User already verified");
        }
        if (!Objects.equals(user.getVerificationCode(), verifyUserRequest.getCode())) {
            throw new GenericException(GenericExceptionCode.INVALID_VERIFICATION_CODE, "Invalid verification code");
        }
        user.setEnabled(true);
        user.setVerificationCode(null);
        userRepository.save(user);

    }

    @Override
    @Transactional
    public void createPasswordResetToken(String userEmail) throws GenericException, MessagingException, UnsupportedEncodingException {
        User user = userRepository.findByEmail(userEmail);
        if (user == null) {
            throw new GenericException(GenericExceptionCode.USER_NOT_FOUND, "No user found with this email!");
        }
        String otp = generateVerificationCode();
        int secondsToAdd = PasswordResetToken.EXPIRATION;
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, secondsToAdd);

        PasswordResetToken myToken = new PasswordResetToken(otp, user, calendar.getTime());
        passwordTokenRepository.save(myToken);
        emailVerificationService.sendResetPassword(userEmail, user.getName(), otp);
    }

    @Override
    @Transactional
    public boolean changeUserPasswordWithResetToken(String email, String password) {
        if (!PasswordValidator.isPasswordStrong(password)) {
            return false;
        }

        PasswordResetToken passwordResetToken = passwordTokenRepository.findTopByUserEmailOrderByExpiryDateDesc(email);
        User user = passwordResetToken.getUser();
        if (user != null) {
            user.setPassword(encoder.encode(password));
            userRepository.save(user);
            passwordResetToken.setUsed(true);
            passwordTokenRepository.save(passwordResetToken);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void validatePasswordResetToken(String token, String email) throws GenericException {
        final PasswordResetToken passToken = passwordTokenRepository.findTopByUserEmailOrderByExpiryDateDesc(email);

        if (isTokenExpired(passToken)) {
            throw new GenericException(GenericExceptionCode.TOKEN_EXPIRED, "Token expired");
        } else if (passToken.isUsed()) {
            throw new GenericException(GenericExceptionCode.TOKEN_ALREADY_USED, "Token already used");
        } else if (!token.equals(passToken.getOtp())) {
            throw new GenericException(GenericExceptionCode.INVALID_TOKEN, "Invalid token");
        }
    }

    @Override
    public User getUserById(Long id) throws GenericException {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            throw new GenericException(GenericExceptionCode.USER_NOT_FOUND, "User not found");
        }
        return user;
    }

    @Override
    public UserParticipantSummary getUserSummary(Long id) {
        if (id == null) {
            return null;
        }
        return userRepository.findSummaryById(id).orElse(null);
    }

    @Override
    public PublicProfileView getPublicProfile(Long id) {
        if (id == null) {
            return null;
        }
        return userRepository.findPublicProfileById(id).orElse(null);
    }

    @Override
    public Set<Language> getUserLanguages(Long id) {
        if (id == null) {
            return Collections.emptySet();
        }
        return new LinkedHashSet<>(userRepository.findLanguagesByUserId(id));
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public Page<User> searchUsers(String search, Long cityId, Pageable pageable) {
        String normalizedSearch = search == null ? null : search.trim();
        Long searchId = parseSearchId(normalizedSearch);
        return userRepository.searchUsers(normalizedSearch, searchId, cityId, pageable);
    }

    private static Long parseSearchId(String search) {
        if (search == null || search.isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(search);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Override
    public void updatePassword(Long userId, UpdatePasswordRequest updatePasswordRequest) throws GenericException {
        User user = userRepository.findById(userId).orElseThrow(() -> new GenericException(GenericExceptionCode.USER_NOT_FOUND, "User not found"));
        if (!PasswordValidator.isPasswordStrong(updatePasswordRequest.getNewPassword())) {
            throw new GenericException(GenericExceptionCode.PASSWORD_NOT_STRONG, "Error: Password is not strong enough!");
        }

        if (!encoder.matches(updatePasswordRequest.getOldPassword(), user.getPassword())) {
            throw new GenericException(GenericExceptionCode.INCORRECT_PASSWORD, "Error: Old password is incorrect!");
        }

        user.setPassword(encoder.encode(updatePasswordRequest.getNewPassword()));

        userRepository.save(user);
    }

    @Override
    public User updateUserRole(UpdateUserRoleRequest updateUserRoleRequest) throws GenericException {
        User user = userRepository.findById(updateUserRoleRequest.getUserId()).orElseThrow(() -> new GenericException(GenericExceptionCode.USER_NOT_FOUND, "User not found"));
        HashSet<Role> roles = new HashSet<>(roleRepository.findAllById(updateUserRoleRequest.getNewRoles()));
        if (roles.isEmpty()) {
            throw new GenericException(GenericExceptionCode.USER_NOT_FOUND, "No roles found");
        }
        user.setRoles(roles);

        return userRepository.save(user);
    }

    @Override
    public List<Role> getAvailableRoles() {
        return roleRepository.findAll();
    }

    @Override
    public User updateUser(UpdateUserRequest updateUserRequest) throws GenericException {
        User user = userRepository.findById(updateUserRequest.getUserId()).orElseThrow(() -> new GenericException(GenericExceptionCode.USER_NOT_FOUND, "User not found"));
        try {
            if (updateUserRequest.getEmail() != null) {
                user.setEmail(updateUserRequest.getEmail());
            }
            return userRepository.save(user);
        } catch (Exception e) {
            throw new GenericException(GenericExceptionCode.EMAIL_ALREADY_EXISTS, "Email already exists");
        }
    }

    @Override
    @Transactional
    public User adminUpdateUser(AdminUpdateUserRequest request) throws GenericException {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new GenericException(GenericExceptionCode.USER_NOT_FOUND, "User not found"));

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getFirstName() != null || request.getLastName() != null) {
            user.setName(buildFullName(user.getFirstName(), user.getLastName()));
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getVerified() != null) {
            user.setEnabled(request.getVerified());
        }
        if (request.getAge() != null) {
            user.setAge(request.getAge());
        }
        if (request.getSex() != null) {
            user.setSex(request.getSex());
        }
        if (request.getLanguages() != null) {
            user.setLanguages(new LinkedHashSet<>(request.getLanguages()));
        }
        if (request.getProfession() != null) {
            user.setProfession(request.getProfession());
        }
        if (request.getSmokingPreference() != null) {
            user.setSmokingPreference(request.getSmokingPreference());
        }
        if (request.getPetPolicy() != null) {
            user.setPetPolicy(request.getPetPolicy());
        }
        if (request.getOccupation() != null) {
            user.setOccupation(request.getOccupation());
        }
        if (request.getShortDescription() != null) {
            user.setShortDescription(request.getShortDescription());
        }

        try {
            if (request.getEmail() != null) {
                user.setEmail(request.getEmail());
            }
            return userRepository.save(user);
        } catch (Exception e) {
            throw new GenericException(GenericExceptionCode.EMAIL_ALREADY_EXISTS, "Email already exists");
        }
    }

    @Override
    @Transactional
    public User setBlocked(Long userId, boolean blocked) throws GenericException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GenericException(GenericExceptionCode.USER_NOT_FOUND, "User not found"));
        user.setBlocked(blocked);
        return userRepository.save(user);
    }

    @Override
    public void sendVerificationCode(String email) throws MessagingException, UnsupportedEncodingException {
        User user = userRepository.findByEmail(email);
        if (user != null && !user.isEnabled()) {
            // Generate a new code
            String code = generateVerificationCode();
            user.setVerificationCode(code);
            userRepository.save(user);

            // Send email
            emailVerificationService.sendVerificationCode(user.getEmail(), user.getName(), code);
        }
    }

    @Override
    @Transactional
    public User updateUserPreferences(Long userId, UpdateUserPreferencesRequest request) throws GenericException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GenericException(GenericExceptionCode.USER_NOT_FOUND, "User not found"));

        LocalDateTime now = LocalDateTime.now();

        if (request.getMarketingConsentAccepted() != null) {
            user.setMarketingConsentAccepted(request.getMarketingConsentAccepted());
            user.setMarketingConsentUpdatedAt(now);
        }

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateProfile(Long userId, UpdateProfileRequest request) throws GenericException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GenericException(GenericExceptionCode.USER_NOT_FOUND, "User not found"));

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getFirstName() != null || request.getLastName() != null) {
            user.setName(buildFullName(user.getFirstName(), user.getLastName()));
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAge() != null) {
            user.setAge(request.getAge());
        }
        if (request.getSex() != null) {
            user.setSex(request.getSex());
        }
        if (request.getLanguages() != null) {
            user.setLanguages(new LinkedHashSet<>(request.getLanguages()));
        }
        if (request.getProfession() != null) {
            user.setProfession(request.getProfession());
        }
        if (request.getSmokingPreference() != null) {
            user.setSmokingPreference(request.getSmokingPreference());
        }
        if (request.getPetPolicy() != null) {
            user.setPetPolicy(request.getPetPolicy());
        }
        if (request.getOccupation() != null) {
            user.setOccupation(request.getOccupation());
        }
        if (request.getShortDescription() != null) {
            user.setShortDescription(request.getShortDescription());
        }

        UserPreferencesDto preferences = user.getPreferences();
        if (preferences == null) {
            preferences = new UserPreferencesDto();
            user.setPreferences(preferences);
        }
        if (request.getPreferredMaxBudget() != null) {
            preferences.setPreferredMaxBudget(request.getPreferredMaxBudget());
        }
        if (request.getPreferredPropertyType() != null) {
            preferences.setPreferredPropertyType(request.getPreferredPropertyType());
        }
        if (request.getPreferredFurnished() != null) {
            preferences.setPreferredFurnished(request.getPreferredFurnished());
        }
        if (request.getPreferredPetsAllowed() != null) {
            preferences.setPreferredPetsAllowed(request.getPreferredPetsAllowed());
        }
        if (request.getPreferredMinBedrooms() != null) {
            preferences.setPreferredMinBedrooms(request.getPreferredMinBedrooms());
        }
        if (request.getPreferredMinBathrooms() != null) {
            preferences.setPreferredMinBathrooms(request.getPreferredMinBathrooms());
        }
        if (request.getPreferredCountryId() != null) {
            Country country = countryRepository.findById(request.getPreferredCountryId())
                    .orElseThrow(() -> new GenericException(GenericExceptionCode.COUNTRY_NOT_FOUND, "Country not found"));
            preferences.setPreferredCountry(country);
        }
        if (request.getPreferredCityId() != null) {
            City city = cityRepository.findById(request.getPreferredCityId())
                    .orElseThrow(() -> new GenericException(GenericExceptionCode.CITY_NOT_FOUND, "City not found"));
            preferences.setPreferredCity(city);
        }
        if (request.getPreferredRoomAmenities() != null) {
            preferences.setPreferredRoomAmenities(new LinkedHashSet<>(request.getPreferredRoomAmenities()));
        }
        if (request.getPreferredNearbyAmenities() != null) {
            preferences.setPreferredNearbyAmenities(new LinkedHashSet<>(request.getPreferredNearbyAmenities()));
        }

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateProfilePhoto(Long userId, MultipartFile file) throws GenericException, IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GenericException(GenericExceptionCode.USER_NOT_FOUND, "User not found"));
        if (file == null || file.isEmpty()) {
            throw new GenericException(GenericExceptionCode.INVALID_FILE, "No file provided");
        }
        ImageUploadValidator.validate(file);
        ImageProcessor.ProcessedImage processed = ImageProcessor.process(
                file.getBytes(), file.getContentType(), ImageProcessor.Preset.PROFILE);
        user.setProfilePhoto(processed.data());
        return userRepository.save(user);
    }

    @Override
    public byte[] getProfilePhoto(Long userId) throws GenericException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GenericException(GenericExceptionCode.USER_NOT_FOUND, "User not found"));
        if (user.getDeletedAt() != null || user.getProfilePhoto() == null || user.getProfilePhoto().length == 0) {
            throw new GenericException(GenericExceptionCode.USER_NOT_FOUND, "Profile photo not found");
        }
        return user.getProfilePhoto();
    }

    @Override
    public boolean isProfileComplete(User user) {
        return user.getProfilePhoto() != null && user.getProfilePhoto().length > 0
                && hasText(user.getFirstName())
                && hasText(user.getLastName())
                && hasText(user.getPhone())
                && user.getAge() != null
                && user.getSex() != null
                && user.getLanguages() != null && !user.getLanguages().isEmpty()
                && user.getOccupation() != null
                && user.getSmokingPreference() != null
                && user.getPetPolicy() != null;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String buildFullName(String firstName, String lastName) {
        return String.join(" ",
                        firstName == null ? "" : firstName.trim(),
                        lastName == null ? "" : lastName.trim())
                .trim();
    }

    private String generateVerificationCode() {
        return String.format("%06d", new Random().nextInt(1_000_000));
    }

    private static String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private boolean isTokenExpired(PasswordResetToken passToken) {
        final Calendar cal = Calendar.getInstance();
        return passToken.getExpiryDate().before(cal.getTime());
    }
}
