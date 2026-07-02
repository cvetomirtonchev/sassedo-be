package server.sassedo.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import server.sassedo.model.GenericException;
import server.sassedo.security.jwt.JwtUtils;
import server.sassedo.user.data.UserDetailsImpl;
import server.sassedo.user.data.dto.Role;
import server.sassedo.user.data.dto.User;
import server.sassedo.user.data.network.UpdateUserRequest;
import server.sassedo.user.data.network.request.UpdatePasswordRequest;
import server.sassedo.user.data.network.request.UpdateUserPreferencesRequest;
import server.sassedo.user.data.network.request.UpdateUserRoleRequest;
import server.sassedo.user.data.network.response.UserResponse;
import server.sassedo.user.data.network.response.UserRolesResponse;
import server.sassedo.user.service.user.UserService;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

import static server.sassedo.utils.ServerUtils.getUserId;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtils jwtUtils;

    @GetMapping("/details")
    public ResponseEntity<?> getUserDetails(HttpServletRequest request) {
        Long userId = getUserId(request, jwtUtils);
        try {
            User user = userService.getUserById(userId);
            return ResponseEntity.ok(mapUserToResponse(user));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/update-password")
    public ResponseEntity<?> updatePassword(@RequestBody UpdatePasswordRequest updatePasswordRequest, HttpServletRequest request) {
        Long userId = getUserId(request, jwtUtils);
        try {
            userService.updatePassword(userId, updatePasswordRequest);
            return ResponseEntity.ok().build();
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateUser(@RequestBody UpdateUserRequest updateUserRequest, HttpServletRequest request) {
        Long userId = getUserId(request, jwtUtils);
        try {
            updateUserRequest.setUserId(userId);
            updateUserRequest.setEmail(null);
            User user = userService.updateUser(updateUserRequest);
            return ResponseEntity.ok(mapUserToResponse(user));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @PutMapping("/update-preferences")
    public ResponseEntity<?> updateUserPreferences(
            @RequestBody UpdateUserPreferencesRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            User user = userService.updateUserPreferences(userId, request);
            return ResponseEntity.ok(mapUserToResponse(user));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @PutMapping("/admin/update")
    public ResponseEntity<?> updateUserByAdmin(@RequestBody UpdateUserRequest updateUserRequest, HttpServletRequest request) {
        try {
            User user = userService.updateUser(updateUserRequest);
            return ResponseEntity.ok(mapUserToResponse(user));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> loadUser(HttpServletRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        UserResponse infoResponse = new UserResponse(
                userDetails.getId(),
                userDetails.getEmail(),
                userDetails.getName()
        );

        return ResponseEntity.ok().body(infoResponse);
    }

    @GetMapping("/admin/all")
    public ResponseEntity<?> getAll(HttpServletRequest request) {
        List<User> user = userService.getAllUsers();
        List<UserResponse> userResponses = user.stream().map(this::mapUserToResponse).collect(Collectors.toList());
        return ResponseEntity.ok(userResponses);
    }

    @GetMapping("/admin/roles")
    public ResponseEntity<?> getAvailableRoles() {
        List<UserRolesResponse> roles = userService.getAvailableRoles().stream().map(role -> new UserRolesResponse(role.getId(), role.getName())).collect(Collectors.toList());
        return ResponseEntity.ok(roles);
    }

    @PutMapping("/admin/update-role")
    public ResponseEntity<?> updateUserRole(@RequestBody UpdateUserRoleRequest updateUserRoleRequest) {
        try {
            UserResponse user = mapUserToResponse(userService.updateUserRole(updateUserRoleRequest));
            return ResponseEntity.ok(user);
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @DeleteMapping
    public ResponseEntity<?> deleteUser(HttpServletRequest request) {
        Long userId = getUserId(request, jwtUtils);
        try {
            userService.deleteUser(userId);
            return ResponseEntity.ok().build();
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    private UserResponse mapUserToResponse(User user) {
        return new UserResponse(user.getId(),
                user.getEmail(),
                user.getName(),
                user.isEnabled(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toList()),
                user.isMarketingConsentAccepted()
        );
    }
}
