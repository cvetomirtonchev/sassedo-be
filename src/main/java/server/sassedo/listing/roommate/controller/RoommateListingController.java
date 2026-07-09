package server.sassedo.listing.roommate.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import server.sassedo.common.data.network.response.PageMeta;
import server.sassedo.common.data.network.response.PagedResponse;
import server.sassedo.engagement.service.EngagementEnricher;
import server.sassedo.engagement.service.ListingViewService;
import server.sassedo.listing.common.ListingContactResponse;
import server.sassedo.listing.common.ListingFilter;
import server.sassedo.listing.common.ListingSort;
import server.sassedo.listing.common.ListingStatus;
import server.sassedo.listing.common.PropertyType;
import server.sassedo.listing.roommate.data.dto.RoommateListing;
import server.sassedo.listing.roommate.data.dto.RoommateListingPhoto;
import server.sassedo.listing.roommate.data.network.request.RoommateListingRequest;
import server.sassedo.listing.roommate.data.network.request.UpdateListingStatusRequest;
import server.sassedo.listing.roommate.data.network.response.RoommateListingResponse;
import server.sassedo.listing.roommate.service.RoommateListingService;
import server.sassedo.model.GenericException;
import server.sassedo.promotion.common.ListingType;
import server.sassedo.security.jwt.JwtUtils;
import server.sassedo.user.data.dto.User;
import server.sassedo.user.service.user.UserService;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static server.sassedo.utils.ServerUtils.getUserId;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/roommate-listings")
@RequiredArgsConstructor
public class RoommateListingController {

    private final RoommateListingService listingService;
    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final RoommateListingMapper mapper;
    private final EngagementEnricher engagementEnricher;
    private final ListingViewService listingViewService;

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
            @RequestParam(required = false) Boolean hasProperty,
            @RequestParam(required = false) PropertyType propertyType,
            @RequestParam(required = false) String neighborhood,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate availableBy,
            @RequestParam(required = false) Integer minBedrooms,
            @RequestParam(required = false) Integer minBathrooms,
            @RequestParam(required = false) Set<String> amenities,
            @RequestParam(required = false) Integer minMatchScore,
            @RequestParam(defaultValue = "NEWEST") ListingSort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        ListingFilter filter = new ListingFilter();
        filter.setCityId(cityId);
        filter.setHasProperty(hasProperty);
        filter.setPropertyType(propertyType);
        filter.setNeighborhood(neighborhood);
        filter.setMinPrice(minPrice);
        filter.setMaxPrice(maxPrice);
        filter.setAvailableBy(availableBy);
        filter.setMinBedrooms(minBedrooms);
        filter.setMinBathrooms(minBathrooms);
        filter.setAmenities(amenities);

        User user = resolveUser(getUserId(httpRequest, jwtUtils));

        // Match-based sort and the minimum-match filter both require scoring every candidate, so they
        // are handled in memory across all matching listings (only meaningful for logged-in users).
        if (user != null && (sort == ListingSort.BEST_MATCH || minMatchScore != null)) {
            return ResponseEntity.ok(inMemoryMatchPage(filter, user, sort, minMatchScore, page, size));
        }

        Page<RoommateListing> listings = listingService.browse(filter,
                PageRequest.of(page, size, sort.toSort("rent")));
        return ResponseEntity.ok(toPagedResponse(listings, user));
    }

    private PagedResponse<RoommateListingResponse> inMemoryMatchPage(ListingFilter filter, User user,
            ListingSort sort, Integer minMatchScore, int page, int size) {
        List<RoommateListingResponse> mapped = listingService.browseAllForMatch(filter).stream()
                .map(listing -> mapToResponse(listing, user))
                .collect(Collectors.toList());

        if (minMatchScore != null) {
            int threshold = minMatchScore;
            mapped = mapped.stream()
                    .filter(r -> r.getMatchScore() != null && r.getMatchScore() >= threshold)
                    .collect(Collectors.toList());
        }

        // When the Match filter is active, always rank by match percentage (highest first),
        // regardless of the selected sort control.
        ListingSort effectiveSort = minMatchScore != null ? ListingSort.BEST_MATCH : sort;
        Comparator<RoommateListingResponse> comparator = Comparator
                .comparingInt(RoommateListingResponse::getPromotionPriority).reversed()
                .thenComparing(matchOrListingComparator(effectiveSort));
        List<RoommateListingResponse> ranked = mapped.stream()
                .sorted(comparator)
                .collect(Collectors.toList());

        int total = ranked.size();
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        List<RoommateListingResponse> content = ranked.subList(from, to);
        content.forEach(r -> enrichOwner(r, r.getOwnerId()));
        engagementEnricher.enrich(ListingType.ROOMMATE, content, user.getId(), false);
        return new PagedResponse<>(content, new PageMeta(page, totalPages, total));
    }

    private Comparator<RoommateListingResponse> matchOrListingComparator(ListingSort sort) {
        Comparator<RoommateListingResponse> byCreatedDesc = Comparator.comparing(
                RoommateListingResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
        return switch (sort) {
            case BEST_MATCH -> Comparator.comparing(
                            (RoommateListingResponse r) -> r.getMatchScore() == null ? -1 : r.getMatchScore())
                    .reversed()
                    .thenComparing(byCreatedDesc);
            case OLDEST -> Comparator.comparing(
                    RoommateListingResponse::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case PRICE_ASC -> Comparator.comparing(
                    RoommateListingResponse::getRent, Comparator.nullsLast(Comparator.naturalOrder()));
            case PRICE_DESC -> Comparator.comparing(
                    RoommateListingResponse::getRent, Comparator.nullsLast(Comparator.reverseOrder()));
            case RECENTLY_UPDATED -> Comparator.comparing(
                    RoommateListingResponse::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
            case NEWEST -> byCreatedDesc;
        };
    }

    private User resolveUser(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            return userService.getUserById(userId);
        } catch (GenericException e) {
            return null;
        }
    }

    @GetMapping("/mine")
    public ResponseEntity<?> myListings(HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        List<RoommateListingResponse> content = listingService.getMyListings(userId).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
        engagementEnricher.enrich(ListingType.ROOMMATE, content, userId, false);
        return ResponseEntity.ok(content);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            RoommateListing listing = listingService.getViewableById(id, userId, isAdmin());
            RoommateListingResponse response = mapToResponse(listing, resolveUser(userId));
            enrichOwner(response, listing.getOwnerId());
            String visitorId = httpRequest.getHeader("X-Visitor-Id");
            listingViewService.recordView(ListingType.ROOMMATE, id, userId, visitorId, listing.getOwnerId());
            engagementEnricher.enrich(ListingType.ROOMMATE, response, userId, true);
            return ResponseEntity.ok(response);
        } catch (GenericException e) {
            return ResponseEntity.status(404).body(e.getErrorResponse());
        }
    }

    @GetMapping("/{id}/contact")
    public ResponseEntity<?> getContact(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            RoommateListing listing = listingService.getViewableById(id, userId, isAdmin());
            User owner = userService.getUserById(listing.getOwnerId());
            return ResponseEntity.ok(new ListingContactResponse(owner.getName(), owner.getPhone()));
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

    @PostMapping("/{id}/renew")
    public ResponseEntity<?> renew(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            RoommateListing listing = listingService.renew(id, userId);
            return ResponseEntity.ok(mapToResponse(listing));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivate(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            RoommateListing listing = listingService.deactivate(id, userId);
            return ResponseEntity.ok(mapToResponse(listing));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<?> reactivate(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            RoommateListing listing = listingService.reactivate(id, userId);
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
        return toPagedResponse(listings, null);
    }

    private PagedResponse<RoommateListingResponse> toPagedResponse(Page<RoommateListing> listings, User user) {
        List<RoommateListingResponse> content = listings.getContent().stream()
                .map(listing -> mapToResponse(listing, user)).collect(Collectors.toList());
        content.forEach(r -> enrichOwner(r, r.getOwnerId()));
        engagementEnricher.enrich(ListingType.ROOMMATE, content, user != null ? user.getId() : null, false);
        PageMeta meta = new PageMeta(listings.getNumber(), listings.getTotalPages(), listings.getTotalElements());
        return new PagedResponse<>(content, meta);
    }

    private RoommateListingResponse mapToResponse(RoommateListing listing) {
        return mapper.map(listing);
    }

    private RoommateListingResponse mapToResponse(RoommateListing listing, User user) {
        return mapper.map(listing, user);
    }

    private void enrichOwner(RoommateListingResponse response, Long ownerId) {
        mapper.enrichOwner(response, ownerId);
    }
}
