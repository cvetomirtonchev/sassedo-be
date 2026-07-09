package server.sassedo.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import server.sassedo.common.data.network.response.PageMeta;
import server.sassedo.common.data.network.response.PagedResponse;
import server.sassedo.model.GenericException;
import server.sassedo.security.jwt.JwtUtils;
import server.sassedo.user.data.UserDetailsImpl;
import server.sassedo.user.data.dto.Role;
import server.sassedo.user.data.dto.User;
import server.sassedo.user.data.dto.UserPreferencesDto;
import server.sassedo.user.data.network.UpdateUserRequest;
import server.sassedo.user.data.network.request.AdminBlockUserRequest;
import server.sassedo.user.data.network.request.AdminUpdateUserRequest;
import server.sassedo.user.data.network.request.UpdatePasswordRequest;
import server.sassedo.user.data.network.request.UpdateProfileRequest;
import server.sassedo.user.data.network.request.UpdateUserPreferencesRequest;
import server.sassedo.user.data.network.request.UpdateUserRoleRequest;
import server.sassedo.user.data.network.response.UserResponse;
import server.sassedo.user.data.network.response.UserRolesResponse;
import server.sassedo.user.service.user.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
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

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateProfileRequest request, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            User user = userService.updateProfile(userId, request);
            return ResponseEntity.ok(mapUserToResponse(user));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @PostMapping(value = "/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadProfilePicture(@RequestParam("file") MultipartFile file, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            User user = userService.updateProfilePhoto(userId, file);
            return ResponseEntity.ok(mapUserToResponse(user));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Could not read uploaded file");
        }
    }

    @GetMapping("/{id}/picture")
    public ResponseEntity<byte[]> getProfilePicture(@PathVariable Long id) {
        try {
            byte[] image = userService.getProfilePhoto(id);
            if (image == null || image.length == 0) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(image);
        } catch (GenericException e) {
            return ResponseEntity.notFound().build();
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
    public ResponseEntity<?> updateUserByAdmin(@Valid @RequestBody AdminUpdateUserRequest updateUserRequest, HttpServletRequest request) {
        try {
            User user = userService.adminUpdateUser(updateUserRequest);
            return ResponseEntity.ok(mapUserToResponse(user));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @PutMapping("/admin/block")
    public ResponseEntity<?> setUserBlocked(@RequestBody AdminBlockUserRequest blockUserRequest) {
        try {
            User user = userService.setBlocked(blockUserRequest.getUserId(), blockUserRequest.isBlocked());
            return ResponseEntity.ok(mapUserToResponse(user));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<?> deleteUserByAdmin(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok().build();
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
    public ResponseEntity<?> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "40") int size) {
        Page<User> users = userService.searchUsers(search, PageRequest.of(page, size, Sort.by("id")));
        List<UserResponse> content = users.getContent().stream()
                .map(this::mapUserToResponse).collect(Collectors.toList());
        PageMeta meta = new PageMeta(users.getNumber(), users.getTotalPages(), users.getTotalElements());
        return ResponseEntity.ok(new PagedResponse<>(content, meta));
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
        UserResponse response = new UserResponse(user.getId(),
                user.getEmail(),
                user.getName(),
                user.isEnabled(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toList()),
                user.isMarketingConsentAccepted()
        );
        response.setBlocked(user.isBlocked());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setPhone(user.getPhone());
        response.setAge(user.getAge());
        response.setSex(user.getSex());
        response.setLanguages(user.getLanguages());
        response.setJobStatus(user.getJobStatus());
        response.setProfession(user.getProfession());
        response.setSmokingPreference(user.getSmokingPreference());
        response.setPetPolicy(user.getPetPolicy());
        response.setOccupation(user.getOccupation());
        response.setShortDescription(user.getShortDescription());

        UserPreferencesDto preferences = user.getPreferences();
        if (preferences != null) {
            response.setPreferredMaxBudget(preferences.getPreferredMaxBudget());
            response.setPreferredPropertyType(preferences.getPreferredPropertyType());
            response.setPreferredFurnished(preferences.getPreferredFurnished());
            response.setPreferredPetsAllowed(preferences.getPreferredPetsAllowed());
            response.setPreferredMinBedrooms(preferences.getPreferredMinBedrooms());
            response.setPreferredMinBathrooms(preferences.getPreferredMinBathrooms());
            response.setPreferredRoomAmenities(preferences.getPreferredRoomAmenities());
            response.setPreferredNearbyAmenities(preferences.getPreferredNearbyAmenities());
            if (preferences.getPreferredCountry() != null) {
                response.setPreferredCountryId(preferences.getPreferredCountry().getId());
                response.setPreferredCountryNameEn(preferences.getPreferredCountry().getNameEn());
                response.setPreferredCountryNameBg(preferences.getPreferredCountry().getNameBg());
            }
            if (preferences.getPreferredCity() != null) {
                response.setPreferredCityId(preferences.getPreferredCity().getId());
                response.setPreferredCityNameEn(preferences.getPreferredCity().getNameEn());
                response.setPreferredCityNameBg(preferences.getPreferredCity().getNameBg());
            }
        }
        response.setProfileComplete(userService.isProfileComplete(user));
        response.setPreferencesComplete(preferences != null && preferences.isCoreComplete());

        if (user.getProfilePhoto() != null && user.getProfilePhoto().length > 0) {
            String photoUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/user/")
                    .path(String.valueOf(user.getId()))
                    .path("/picture")
                    .toUriString();
            response.setProfilePhotoUrl(photoUrl);
        }

        return response;
    }
}
