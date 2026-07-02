package server.sassedo.user.service.user;

import server.sassedo.model.GenericException;
import server.sassedo.user.data.dto.Role;
import server.sassedo.user.data.dto.User;
import server.sassedo.user.data.network.UpdateUserRequest;
import server.sassedo.user.data.network.request.RegisterRequest;
import server.sassedo.user.data.network.request.UpdatePasswordRequest;
import server.sassedo.user.data.network.request.UpdateUserPreferencesRequest;
import server.sassedo.user.data.network.request.UpdateUserRoleRequest;
import server.sassedo.user.data.network.request.VerifyUserRequest;

import jakarta.mail.MessagingException;
import java.io.UnsupportedEncodingException;
import java.util.List;

public interface UserService {
    void registerUser(RegisterRequest signUpRequest, String siteURL) throws UnsupportedEncodingException, MessagingException, GenericException;

    void verify(VerifyUserRequest verifyUserRequest) throws GenericException;

    void createPasswordResetToken(String userEmail) throws GenericException, MessagingException, UnsupportedEncodingException;

    boolean changeUserPasswordWithResetToken(String email, String password);

    void validatePasswordResetToken(String otp, String email) throws GenericException;

    User getUserById(Long id) throws GenericException;

    User getUserByEmail(String email) throws GenericException;

    List<User> getAllUsers();

    void updatePassword(Long userId, UpdatePasswordRequest updatePasswordRequest) throws GenericException;

    User updateUserRole(UpdateUserRoleRequest updateUserRoleRequest) throws GenericException;

    List<Role> getAvailableRoles();

    User updateUser(UpdateUserRequest updateUserRequest) throws GenericException;

    void sendVerificationCode(String email) throws MessagingException, UnsupportedEncodingException;

    void deleteUser(Long userId) throws GenericException;

    User updateUserPreferences(Long userId, UpdateUserPreferencesRequest request) throws GenericException;
}
