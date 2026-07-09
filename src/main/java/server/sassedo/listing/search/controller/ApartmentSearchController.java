package server.sassedo.listing.search.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import server.sassedo.common.data.network.response.PageMeta;
import server.sassedo.common.data.network.response.PagedResponse;
import server.sassedo.engagement.service.EngagementEnricher;
import server.sassedo.engagement.service.ListingViewService;
import server.sassedo.listing.common.LeaseTerm;
import server.sassedo.listing.common.ListingContactResponse;
import server.sassedo.listing.common.ListingFilter;
import server.sassedo.listing.common.ListingSort;
import server.sassedo.listing.common.ListingStatus;
import server.sassedo.listing.common.PropertyType;
import server.sassedo.listing.roommate.data.network.request.UpdateListingStatusRequest;
import server.sassedo.listing.search.data.dto.ApartmentSearch;
import server.sassedo.listing.search.data.network.request.ApartmentSearchRequest;
import server.sassedo.listing.search.data.network.response.ApartmentSearchResponse;
import server.sassedo.listing.search.service.ApartmentSearchService;
import server.sassedo.model.GenericException;
import server.sassedo.promotion.common.ListingType;
import server.sassedo.security.jwt.JwtUtils;
import server.sassedo.user.data.dto.User;
import server.sassedo.user.service.user.UserService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static server.sassedo.utils.ServerUtils.getUserId;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/apartment-searches")
@RequiredArgsConstructor
public class ApartmentSearchController {

    private final ApartmentSearchService searchService;
    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final ApartmentSearchMapper mapper;
    private final EngagementEnricher engagementEnricher;
    private final ListingViewService listingViewService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ApartmentSearchRequest request, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            ApartmentSearch entity = searchService.create(userId, request);
            return ResponseEntity.ok(mapToResponse(entity));
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
            @RequestParam(required = false) Set<LeaseTerm> leaseTerms,
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
        filter.setLeaseTerms(leaseTerms);
        Page<ApartmentSearch> results = searchService.browse(filter,
                PageRequest.of(page, size, sort.toSort("budgetMax")));
        return ResponseEntity.ok(toPagedResponse(results, getUserId(httpRequest, jwtUtils)));
    }

    @GetMapping("/mine")
    public ResponseEntity<?> myListings(HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        List<ApartmentSearchResponse> content = searchService.getMyListings(userId).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
        engagementEnricher.enrich(ListingType.SEARCH, content, userId, false);
        return ResponseEntity.ok(content);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            ApartmentSearch entity = searchService.getViewableById(id, userId, isAdmin());
            ApartmentSearchResponse response = mapToResponse(entity);
            enrichOwner(response, entity.getOwnerId());
            String visitorId = httpRequest.getHeader("X-Visitor-Id");
            listingViewService.recordView(ListingType.SEARCH, id, userId, visitorId, entity.getOwnerId());
            engagementEnricher.enrich(ListingType.SEARCH, response, userId, true);
            return ResponseEntity.ok(response);
        } catch (GenericException e) {
            return ResponseEntity.status(404).body(e.getErrorResponse());
        }
    }

    @GetMapping("/{id}/contact")
    public ResponseEntity<?> getContact(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            ApartmentSearch entity = searchService.getViewableById(id, userId, isAdmin());
            User owner = userService.getUserById(entity.getOwnerId());
            return ResponseEntity.ok(new ListingContactResponse(owner.getName(), owner.getPhone()));
        } catch (GenericException e) {
            return ResponseEntity.status(404).body(e.getErrorResponse());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ApartmentSearchRequest request, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            ApartmentSearch entity = searchService.update(id, userId, false, request);
            return ResponseEntity.ok(mapToResponse(entity));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @PostMapping("/{id}/renew")
    public ResponseEntity<?> renew(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            ApartmentSearch entity = searchService.renew(id, userId);
            return ResponseEntity.ok(mapToResponse(entity));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivate(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            ApartmentSearch entity = searchService.deactivate(id, userId);
            return ResponseEntity.ok(mapToResponse(entity));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<?> reactivate(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            ApartmentSearch entity = searchService.reactivate(id, userId);
            return ResponseEntity.ok(mapToResponse(entity));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @GetMapping("/admin/all")
    public ResponseEntity<?> adminAll(
            @RequestParam(required = false) ListingStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "40") int size) {
        Page<ApartmentSearch> results = searchService.adminSearch(status, search,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(toPagedResponse(results));
    }

    @PutMapping("/admin/{id}")
    public ResponseEntity<?> adminUpdate(@PathVariable Long id, @RequestBody ApartmentSearchRequest request) {
        try {
            ApartmentSearch entity = searchService.update(id, null, true, request);
            return ResponseEntity.ok(mapToResponse(entity));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @PatchMapping("/admin/{id}/status")
    public ResponseEntity<?> adminSetStatus(@PathVariable Long id, @Valid @RequestBody UpdateListingStatusRequest request) {
        try {
            ApartmentSearch entity = searchService.setStatus(id, request.getStatus());
            return ResponseEntity.ok(mapToResponse(entity));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<?> adminDelete(@PathVariable Long id) {
        try {
            searchService.delete(id);
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

    private PagedResponse<ApartmentSearchResponse> toPagedResponse(Page<ApartmentSearch> results) {
        return toPagedResponse(results, null);
    }

    private PagedResponse<ApartmentSearchResponse> toPagedResponse(Page<ApartmentSearch> results, Long userId) {
        List<ApartmentSearchResponse> content = results.getContent().stream()
                .map(this::mapToResponse).collect(Collectors.toList());
        engagementEnricher.enrich(ListingType.SEARCH, content, userId, false);
        PageMeta meta = new PageMeta(results.getNumber(), results.getTotalPages(), results.getTotalElements());
        return new PagedResponse<>(content, meta);
    }

    private ApartmentSearchResponse mapToResponse(ApartmentSearch entity) {
        return mapper.map(entity);
    }

    private void enrichOwner(ApartmentSearchResponse response, Long ownerId) {
        mapper.enrichOwner(response, ownerId);
    }
}
