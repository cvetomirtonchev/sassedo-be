package server.sassedo.listing.roommate.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
import server.sassedo.listing.common.ListingStatus;
import server.sassedo.listing.common.PropertyType;
import server.sassedo.listing.roommate.data.dto.RoommateListing;
import server.sassedo.listing.roommate.data.dto.RoommateListingPhoto;
import server.sassedo.listing.roommate.data.network.request.RoommateListingRequest;
import server.sassedo.listing.roommate.data.network.request.UpdateListingStatusRequest;
import server.sassedo.listing.roommate.data.network.response.ListingPhotoResponse;
import server.sassedo.listing.roommate.data.network.response.RoommateListingResponse;
import server.sassedo.listing.roommate.service.RoommateListingService;
import server.sassedo.model.GenericException;
import server.sassedo.security.jwt.JwtUtils;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static server.sassedo.utils.ServerUtils.getUserId;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/roommate-listings")
@RequiredArgsConstructor
public class RoommateListingController {

    private final RoommateListingService listingService;
    private final JwtUtils jwtUtils;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody RoommateListingRequest request, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            RoommateListing listing = listingService.create(userId, request);
            return ResponseEntity.ok(mapToResponse(listing));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @GetMapping
    public ResponseEntity<?> browse(
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) PropertyType propertyType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<RoommateListing> listings = listingService.browse(cityId, propertyType,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(toPagedResponse(listings));
    }

    @GetMapping("/mine")
    public ResponseEntity<?> myListings(HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        List<RoommateListingResponse> content = listingService.getMyListings(userId).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
        return ResponseEntity.ok(content);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            RoommateListing listing = listingService.getViewableById(id, userId, isAdmin());
            return ResponseEntity.ok(mapToResponse(listing));
        } catch (GenericException e) {
            return ResponseEntity.status(404).body(e.getErrorResponse());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody RoommateListingRequest request, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            RoommateListing listing = listingService.update(id, userId, false, request);
            return ResponseEntity.ok(mapToResponse(listing));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @PostMapping(value = "/{id}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadPhotos(
            @PathVariable Long id,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "mainIndex", required = false) Integer mainIndex,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            RoommateListing listing = listingService.addPhotos(id, userId, files, mainIndex);
            return ResponseEntity.ok(mapToResponse(listing));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Could not read uploaded file");
        }
    }

    @GetMapping("/{id}/picture")
    public ResponseEntity<byte[]> getMainPicture(@PathVariable Long id) {
        try {
            RoommateListingPhoto photo = listingService.getMainPhoto(id);
            return photoResponse(photo);
        } catch (GenericException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/photos/{photoId}")
    public ResponseEntity<byte[]> getPhoto(@PathVariable Long photoId) {
        try {
            RoommateListingPhoto photo = listingService.getPhoto(photoId);
            return photoResponse(photo);
        } catch (GenericException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/admin/all")
    public ResponseEntity<?> adminAll(
            @RequestParam(required = false) ListingStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "40") int size) {
        Page<RoommateListing> listings = listingService.adminSearch(status, search,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(toPagedResponse(listings));
    }

    @PutMapping("/admin/{id}")
    public ResponseEntity<?> adminUpdate(@PathVariable Long id, @RequestBody RoommateListingRequest request) {
        try {
            RoommateListing listing = listingService.update(id, null, true, request);
            return ResponseEntity.ok(mapToResponse(listing));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @PatchMapping("/admin/{id}/status")
    public ResponseEntity<?> adminSetStatus(@PathVariable Long id, @Valid @RequestBody UpdateListingStatusRequest request) {
        try {
            RoommateListing listing = listingService.setStatus(id, request.getStatus());
            return ResponseEntity.ok(mapToResponse(listing));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<?> adminDelete(@PathVariable Long id) {
        try {
            listingService.delete(id);
            return ResponseEntity.ok().build();
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private ResponseEntity<byte[]> photoResponse(RoommateListingPhoto photo) {
        if (photo.getData() == null || photo.getData().length == 0) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType = MediaType.IMAGE_JPEG;
        if (photo.getContentType() != null) {
            try {
                mediaType = MediaType.parseMediaType(photo.getContentType());
            } catch (Exception ignored) {
                // fall back to jpeg
            }
        }
        return ResponseEntity.ok().contentType(mediaType).body(photo.getData());
    }

    private PagedResponse<RoommateListingResponse> toPagedResponse(Page<RoommateListing> listings) {
        List<RoommateListingResponse> content = listings.getContent().stream()
                .map(this::mapToResponse).collect(Collectors.toList());
        PageMeta meta = new PageMeta(listings.getNumber(), listings.getTotalPages(), listings.getTotalElements());
        return new PagedResponse<>(content, meta);
    }

    private RoommateListingResponse mapToResponse(RoommateListing listing) {
        RoommateListingResponse r = new RoommateListingResponse();
        r.setId(listing.getId());
        r.setOwnerId(listing.getOwnerId());
        r.setStatus(listing.getStatus());
        r.setCreatedAt(listing.getCreatedAt());
        r.setUpdatedAt(listing.getUpdatedAt());
        r.setPropertyType(listing.getPropertyType());

        if (listing.getCountry() != null) {
            r.setCountryId(listing.getCountry().getId());
            r.setCountryNameEn(listing.getCountry().getNameEn());
            r.setCountryNameBg(listing.getCountry().getNameBg());
        }
        if (listing.getCity() != null) {
            r.setCityId(listing.getCity().getId());
            r.setCityNameEn(listing.getCity().getNameEn());
            r.setCityNameBg(listing.getCity().getNameBg());
        }
        r.setNeighborhood(listing.getNeighborhood());
        r.setAddress(listing.getAddress());

        r.setRent(listing.getRent());
        r.setCostsIncluded(listing.getCostsIncluded());
        r.setDeposit(listing.getDeposit());
        r.setRoomsCount(listing.getRoomsCount());
        r.setFurnished(listing.getFurnished());
        r.setPetsAllowed(listing.getPetsAllowed());
        r.setAvailableFrom(listing.getAvailableFrom());
        r.setAvailableAsap(listing.isAvailableAsap());
        r.setBedrooms(listing.getBedrooms());
        r.setBathrooms(listing.getBathrooms());
        r.setOwner(listing.getOwner());

        r.setIncludedUtilities(listing.getIncludedUtilities());
        r.setExtraServices(listing.getExtraServices());
        r.setNearbyAmenities(listing.getNearbyAmenities());
        r.setRoomAmenities(listing.getRoomAmenities());

        r.setPreferredSex(listing.getPreferredSex());
        r.setPreferredOrientation(listing.getPreferredOrientation());
        r.setAgeMin(listing.getAgeMin());
        r.setAgeMax(listing.getAgeMax());
        r.setSmokingPreference(listing.getSmokingPreference());
        r.setOccupationPreference(listing.getOccupationPreference());
        r.setAdditionalRequirements(listing.getAdditionalRequirements());
        r.setPetPolicy(listing.getPetPolicy());
        r.setPeopleInProperty(listing.getPeopleInProperty());
        r.setSpokenLanguages(listing.getSpokenLanguages());
        r.setEmploymentStatus(listing.getEmploymentStatus());
        r.setAboutMe(listing.getAboutMe());

        r.setTitle(listing.getTitle());
        r.setDescription(listing.getDescription());

        List<ListingPhotoResponse> photos = listing.getPhotos().stream()
                .map(p -> new ListingPhotoResponse(p.getId(), buildPhotoUrl(p.getId()), p.isMain()))
                .collect(Collectors.toList());
        r.setPhotos(photos);
        photos.stream().filter(ListingPhotoResponse::isMain).findFirst()
                .ifPresent(main -> r.setMainPhotoUrl(main.getUrl()));
        if (r.getMainPhotoUrl() == null && !photos.isEmpty()) {
            r.setMainPhotoUrl(photos.get(0).getUrl());
        }
        return r;
    }

    private String buildPhotoUrl(Long photoId) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/roommate-listings/photos/")
                .path(String.valueOf(photoId))
                .toUriString();
    }
}
