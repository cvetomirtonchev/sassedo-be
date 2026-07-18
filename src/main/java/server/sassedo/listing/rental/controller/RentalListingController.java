package server.sassedo.listing.rental.controller;

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
import server.sassedo.listing.common.LeaseTerm;
import server.sassedo.listing.common.ListingFilter;
import server.sassedo.listing.common.ListingSort;
import server.sassedo.listing.common.ListingStatus;
import server.sassedo.listing.common.PropertyType;
import server.sassedo.listing.rental.data.dto.RentalListing;
import server.sassedo.listing.rental.data.dto.RentalListingPhoto;
import server.sassedo.listing.common.ListingContactResponse;
import server.sassedo.listing.rental.data.network.request.RentalListingRequest;
import server.sassedo.listing.rental.data.network.response.RentalListingResponse;
import server.sassedo.listing.rental.service.RentalListingService;
import server.sassedo.listing.roommate.data.network.request.UpdateListingStatusRequest;
import server.sassedo.model.GenericException;
import server.sassedo.moderation.risk.web.ClientIpResolver;
import server.sassedo.moderation.risk.web.dto.SubmissionResponse;
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
@RequestMapping("/api/rental-listings")
@RequiredArgsConstructor
public class RentalListingController {

    private final RentalListingService listingService;
    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final RentalListingMapper mapper;
    private final EngagementEnricher engagementEnricher;
    private final ListingViewService listingViewService;
    private final ClientIpResolver clientIpResolver;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody RentalListingRequest request, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            RentalListing listing = listingService.create(userId, request);
            return ResponseEntity.ok(mapToResponse(listing));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @GetMapping
    public ResponseEntity<?> browse(
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) PropertyType propertyType,
            @RequestParam(required = false) String neighborhood,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate availableBy,
            @RequestParam(required = false) Integer minBedrooms,
            @RequestParam(required = false) Integer minBathrooms,
            @RequestParam(required = false) Boolean sharedBedroom,
            @RequestParam(required = false) Boolean sharedBathroom,
            @RequestParam(required = false) Set<LeaseTerm> leaseTerms,
            @RequestParam(required = false) Set<String> amenities,
            @RequestParam(required = false) Integer minMatchScore,
            @RequestParam(defaultValue = "NEWEST") ListingSort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        ListingFilter filter = new ListingFilter();
        filter.setCityId(cityId);
        filter.setPropertyType(propertyType);
        filter.setNeighborhood(neighborhood);
        filter.setMinPrice(minPrice);
        filter.setMaxPrice(maxPrice);
        filter.setAvailableBy(availableBy);
        filter.setMinBedrooms(minBedrooms);
        filter.setMinBathrooms(minBathrooms);
        filter.setSharedBedroom(sharedBedroom);
        filter.setSharedBathroom(sharedBathroom);
        filter.setLeaseTerms(leaseTerms);
        filter.setAmenities(amenities);

        User user = resolveUser(getUserId(httpRequest, jwtUtils));

        // Match-based sort and the minimum-match filter both require scoring every candidate, so they
        // are handled in memory across all matching listings (only meaningful for logged-in users).
        if (user != null && (sort == ListingSort.BEST_MATCH || minMatchScore != null)) {
            return ResponseEntity.ok(inMemoryMatchPage(filter, user, sort, minMatchScore, page, size));
        }

        Page<RentalListing> listings = listingService.browse(filter,
                PageRequest.of(page, size, sort.toSort("rent")));
        return ResponseEntity.ok(toPagedResponse(listings, user));
    }

    private PagedResponse<RentalListingResponse> inMemoryMatchPage(ListingFilter filter, User user,
            ListingSort sort, Integer minMatchScore, int page, int size) {
        List<RentalListingResponse> mapped = listingService.browseAllForMatch(filter).stream()
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
        Comparator<RentalListingResponse> comparator = Comparator
                .comparingInt(RentalListingResponse::getPromotionPriority).reversed()
                .thenComparing(matchOrListingComparator(effectiveSort));
        List<RentalListingResponse> ranked = mapped.stream()
                .sorted(comparator)
                .collect(Collectors.toList());

        int total = ranked.size();
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        List<RentalListingResponse> content = ranked.subList(from, to);
        engagementEnricher.enrich(ListingType.RENTAL, content, user.getId(), false);
        return new PagedResponse<>(content, new PageMeta(page, totalPages, total));
    }

    private Comparator<RentalListingResponse> matchOrListingComparator(ListingSort sort) {
        Comparator<RentalListingResponse> byCreatedDesc = Comparator.comparing(
                RentalListingResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
        return switch (sort) {
            case BEST_MATCH -> Comparator.comparing(
                            (RentalListingResponse r) -> r.getMatchScore() == null ? -1 : r.getMatchScore())
                    .reversed()
                    .thenComparing(byCreatedDesc);
            case OLDEST -> Comparator.comparing(
                    RentalListingResponse::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case PRICE_ASC -> Comparator.comparing(
                    RentalListingResponse::getRent, Comparator.nullsLast(Comparator.naturalOrder()));
            case PRICE_DESC -> Comparator.comparing(
                    RentalListingResponse::getRent, Comparator.nullsLast(Comparator.reverseOrder()));
            case RECENTLY_UPDATED -> Comparator.comparing(
                    RentalListingResponse::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
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

    @GetMapping("/random")
    public ResponseEntity<?> random(
            @RequestParam(defaultValue = "8") int limit, HttpServletRequest httpRequest) {
        int safeLimit = Math.min(Math.max(limit, 1), 24);
        User user = resolveUser(getUserId(httpRequest, jwtUtils));
        List<RentalListingResponse> content = listingService.randomActive(safeLimit).stream()
                .map(listing -> mapToResponse(listing, user))
                .collect(Collectors.toList());
        engagementEnricher.enrich(ListingType.RENTAL, content, user != null ? user.getId() : null, false);
        return ResponseEntity.ok(content);
    }

    @GetMapping("/mine")
    public ResponseEntity<?> myListings(HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        List<RentalListingResponse> content = listingService.getMyListings(userId).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
        engagementEnricher.enrich(ListingType.RENTAL, content, userId, false);
        return ResponseEntity.ok(content);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            RentalListing listing = listingService.getViewableById(id, userId, isAdmin());
            RentalListingResponse response = mapToResponse(listing, resolveUser(userId));
            enrichOwner(response, listing.getOwnerId());
            String visitorId = httpRequest.getHeader("X-Visitor-Id");
            listingViewService.recordView(ListingType.RENTAL, id, userId, visitorId, listing.getOwnerId());
            engagementEnricher.enrich(ListingType.RENTAL, response, userId, true);
            return ResponseEntity.ok(response);
        } catch (GenericException e) {
            return ResponseEntity.status(404).body(e.getErrorResponse());
        }
    }

    @GetMapping("/{id}/contact")
    public ResponseEntity<?> getContact(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            RentalListing listing = listingService.getViewableById(id, userId, isAdmin());
            User owner = userService.getUserById(listing.getOwnerId());
            return ResponseEntity.ok(new ListingContactResponse(owner.getName(), owner.getPhone()));
        } catch (GenericException e) {
            return ResponseEntity.status(404).body(e.getErrorResponse());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody RentalListingRequest request, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            RentalListing listing = listingService.update(id, userId, false, request);
            return ResponseEntity.ok(mapToResponse(listing));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<?> submit(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            SubmissionResponse response = SubmissionResponse.from(
                    listingService.submit(id, userId, clientIpResolver.resolve(httpRequest)));
            return ResponseEntity.ok(response);
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @PostMapping("/{id}/renew")
    public ResponseEntity<?> renew(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            RentalListing listing = listingService.renew(id, userId);
            return ResponseEntity.ok(mapToResponse(listing));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivate(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            RentalListing listing = listingService.deactivate(id, userId);
            return ResponseEntity.ok(mapToResponse(listing));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<?> reactivate(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            RentalListing listing = listingService.reactivate(id, userId);
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
            RentalListing listing = listingService.addPhotos(id, userId, files, mainIndex);
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
            return photoResponse(listingService.getMainPhoto(id));
        } catch (GenericException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/photos/{photoId}")
    public ResponseEntity<byte[]> getPhoto(@PathVariable Long photoId) {
        try {
            return photoResponse(listingService.getPhoto(photoId));
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
        Page<RentalListing> listings = listingService.adminSearch(status, search,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(toPagedResponse(listings));
    }

    @PutMapping("/admin/{id}")
    public ResponseEntity<?> adminUpdate(@PathVariable Long id, @RequestBody RentalListingRequest request) {
        try {
            RentalListing listing = listingService.update(id, null, true, request);
            return ResponseEntity.ok(mapToResponse(listing));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @PatchMapping("/admin/{id}/status")
    public ResponseEntity<?> adminSetStatus(@PathVariable Long id, @Valid @RequestBody UpdateListingStatusRequest request) {
        try {
            RentalListing listing = listingService.setStatus(id, request.getStatus());
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

    private ResponseEntity<byte[]> photoResponse(RentalListingPhoto photo) {
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

    private PagedResponse<RentalListingResponse> toPagedResponse(Page<RentalListing> listings) {
        return toPagedResponse(listings, null);
    }

    private PagedResponse<RentalListingResponse> toPagedResponse(Page<RentalListing> listings, User user) {
        List<RentalListingResponse> content = listings.getContent().stream()
                .map(listing -> mapToResponse(listing, user)).collect(Collectors.toList());
        engagementEnricher.enrich(ListingType.RENTAL, content, user != null ? user.getId() : null, false);
        PageMeta meta = new PageMeta(listings.getNumber(), listings.getTotalPages(), listings.getTotalElements());
        return new PagedResponse<>(content, meta);
    }

    private RentalListingResponse mapToResponse(RentalListing listing) {
        return mapper.map(listing);
    }

    private RentalListingResponse mapToResponse(RentalListing listing, User user) {
        return mapper.map(listing, user);
    }

    private void enrichOwner(RentalListingResponse response, Long ownerId) {
        mapper.enrichOwner(response, ownerId);
    }
}
