package server.sassedo.user.service.user;

import server.sassedo.model.GenericException;
import server.sassedo.user.data.dto.Role;
import server.sassedo.user.data.dto.User;
import org.springframework.web.multipart.MultipartFile;
import server.sassedo.user.data.network.UpdateUserRequest;
import server.sassedo.user.data.network.request.AdminUpdateUserRequest;
import server.sassedo.user.data.network.request.RegisterRequest;
import server.sassedo.user.data.network.request.UpdatePasswordRequest;
import server.sassedo.user.data.network.request.UpdateProfileRequest;
import server.sassedo.user.data.network.request.UpdateUserPreferencesRequest;
import server.sassedo.user.data.network.request.UpdateUserRoleRequest;
import server.sassedo.user.data.network.request.VerifyUserRequest;
import server.sassedo.user.data.dto.Language;
import server.sassedo.user.data.projection.PublicProfileView;
import server.sassedo.user.data.projection.UserParticipantSummary;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;

import jakarta.mail.MessagingException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

public interface UserService {
    void registerUser(RegisterRequest signUpRequest, String siteURL) throws UnsupportedEncodingException, MessagingException, GenericException;

    void verify(VerifyUserRequest verifyUserRequest) throws GenericException;

    void createPasswordResetToken(String userEmail) throws GenericException, MessagingException, UnsupportedEncodingException;

    boolean changeUserPasswordWithResetToken(String email, String password);

    void validatePasswordResetToken(String otp, String email) throws GenericException;

    User getUserById(Long id) throws GenericException;

    /**
     * Lightweight owner/participant summary (id, name, hasPhoto) that never loads the profile-photo
     * blob. Returns {@code null} when the user does not exist. Used to enrich listing responses
     * without pulling image bytes into heap for every card.
     */
    UserParticipantSummary getUserSummary(Long id);

    /**
     * Publicly shareable profile attributes (scalar fields only, no photo blob) for the given user,
     * used to enrich a roommate listing detail response. Returns {@code null} when the user does not
     * exist.
     */
    PublicProfileView getPublicProfile(Long id);

    /**
     * The given user's spoken languages, fetched without loading the rest of the profile.
     */
    Set<Language> getUserLanguages(Long id);

    User getUserByEmail(String email) throws GenericException;

    List<User> getAllUsers();

    Page<User> searchUsers(String search, Long cityId, Pageable pageable);

    void updatePassword(Long userId, UpdatePasswordRequest updatePasswordRequest) throws GenericException;

    User updateUserRole(UpdateUserRoleRequest updateUserRoleRequest) throws GenericException;

    List<Role> getAvailableRoles();

    User updateUser(UpdateUserRequest updateUserRequest) throws GenericException;

    User adminUpdateUser(AdminUpdateUserRequest request) throws GenericException;

    User setBlocked(Long userId, boolean blocked) throws GenericException;

    void sendVerificationCode(String email) throws MessagingException, UnsupportedEncodingException;

    User updateUserPreferences(Long userId, UpdateUserPreferencesRequest request) throws GenericException;

    User updateProfile(Long userId, UpdateProfileRequest request) throws GenericException;

    User updateProfilePhoto(Long userId, MultipartFile file) throws GenericException, IOException;

    byte[] getProfilePhoto(Long userId) throws GenericException;

    boolean isProfileComplete(User user);
}
