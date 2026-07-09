package server.sassedo.engagement.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.sassedo.engagement.data.dto.Favorite;
import server.sassedo.engagement.data.network.request.FavoriteRequest;
import server.sassedo.engagement.data.network.response.FavoriteToggleResponse;
import server.sassedo.engagement.data.network.response.MyFavoritesResponse;
import server.sassedo.engagement.service.EngagementEnricher;
import server.sassedo.engagement.service.FavoriteService;
import server.sassedo.listing.rental.controller.RentalListingMapper;
import server.sassedo.listing.rental.data.dto.RentalListing;
import server.sassedo.listing.rental.data.network.response.RentalListingResponse;
import server.sassedo.listing.rental.service.RentalListingService;
import server.sassedo.listing.roommate.controller.RoommateListingMapper;
import server.sassedo.listing.roommate.data.dto.RoommateListing;
import server.sassedo.listing.roommate.data.network.response.RoommateListingResponse;
import server.sassedo.listing.roommate.service.RoommateListingService;
import server.sassedo.listing.search.controller.ApartmentSearchMapper;
import server.sassedo.listing.search.data.dto.ApartmentSearch;
import server.sassedo.listing.search.data.network.response.ApartmentSearchResponse;
import server.sassedo.listing.search.service.ApartmentSearchService;
import server.sassedo.model.GenericException;
import server.sassedo.promotion.common.ListingType;
import server.sassedo.security.jwt.JwtUtils;
import server.sassedo.user.data.dto.User;
import server.sassedo.user.service.user.UserService;

import java.util.ArrayList;
import java.util.List;

import static server.sassedo.utils.ServerUtils.getUserId;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final EngagementEnricher engagementEnricher;
    private final JwtUtils jwtUtils;
    private final UserService userService;

    private final RentalListingService rentalListingService;
    private final RoommateListingService roommateListingService;
    private final ApartmentSearchService apartmentSearchService;
    private final RentalListingMapper rentalMapper;
    private final RoommateListingMapper roommateMapper;
    private final ApartmentSearchMapper searchMapper;

    @PostMapping
    public ResponseEntity<?> add(@RequestBody FavoriteRequest request, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        if (request.getListingType() == null || request.getListingId() == null) {
            return ResponseEntity.badRequest().build();
        }
        long count = favoriteService.add(userId, request.getListingType(), request.getListingId());
        return ResponseEntity.ok(new FavoriteToggleResponse(true, count));
    }

    @DeleteMapping("/{listingType}/{listingId}")
    public ResponseEntity<?> remove(@PathVariable ListingType listingType, @PathVariable Long listingId,
                                    HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        long count = favoriteService.remove(userId, listingType, listingId);
        return ResponseEntity.ok(new FavoriteToggleResponse(false, count));
    }

    @GetMapping
    public ResponseEntity<?> myFavorites(HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        User user = resolveUser(userId);

        List<RentalListingResponse> rentals = new ArrayList<>();
        for (Favorite favorite : favoriteService.listMine(userId, ListingType.RENTAL)) {
            try {
                RentalListing listing = rentalListingService.getById(favorite.getListingId());
                RentalListingResponse response = rentalMapper.map(listing, user);
                rentalMapper.enrichOwner(response, listing.getOwnerId());
                rentals.add(response);
            } catch (GenericException ignored) {
                // listing was deleted; skip it
            }
        }

        List<RoommateListingResponse> roommates = new ArrayList<>();
        for (Favorite favorite : favoriteService.listMine(userId, ListingType.ROOMMATE)) {
            try {
                RoommateListing listing = roommateListingService.getById(favorite.getListingId());
                RoommateListingResponse response = roommateMapper.map(listing, user);
                roommateMapper.enrichOwner(response, listing.getOwnerId());
                roommates.add(response);
            } catch (GenericException ignored) {
                // listing was deleted; skip it
            }
        }

        List<ApartmentSearchResponse> searches = new ArrayList<>();
        for (Favorite favorite : favoriteService.listMine(userId, ListingType.SEARCH)) {
            try {
                ApartmentSearch entity = apartmentSearchService.getById(favorite.getListingId());
                ApartmentSearchResponse response = searchMapper.map(entity);
                searchMapper.enrichOwner(response, entity.getOwnerId());
                searches.add(response);
            } catch (GenericException ignored) {
                // listing was deleted; skip it
            }
        }

        engagementEnricher.enrich(ListingType.RENTAL, rentals, userId, false);
        engagementEnricher.enrich(ListingType.ROOMMATE, roommates, userId, false);
        engagementEnricher.enrich(ListingType.SEARCH, searches, userId, false);

        return ResponseEntity.ok(new MyFavoritesResponse(rentals, roommates, searches));
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
}
