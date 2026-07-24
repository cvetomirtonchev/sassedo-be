package server.sassedo.listing.rental.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import server.sassedo.listing.common.ListingOwnerEditPolicy;
import server.sassedo.listing.common.PetPolicy;
import server.sassedo.listing.common.matching.PreferenceMatcher;
import server.sassedo.listing.rental.data.dto.RentalListing;
import server.sassedo.listing.rental.data.network.response.RentalListingResponse;
import server.sassedo.listing.rental.matching.RentalMatchScorer;
import server.sassedo.listing.rental.repository.RentalListingPhotoRepository;
import server.sassedo.listing.roommate.data.network.response.ListingPhotoResponse;
import server.sassedo.user.data.dto.User;
import server.sassedo.user.data.projection.UserParticipantSummary;
import server.sassedo.user.service.user.UserService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Entity -> response mapping for rental listings (repo convention: manual mapping, no MapStruct).
 * Extracted from the controller so the same mapping can be reused by the favorites endpoints.
 */
@Component
@RequiredArgsConstructor
public class RentalListingMapper {

    private final UserService userService;
    private final RentalMatchScorer matchScorer;
    private final RentalListingPhotoRepository photoRepository;

    public RentalListingResponse map(RentalListing listing) {
        return map(listing, null);
    }

    public RentalListingResponse map(RentalListing listing, User user) {
        RentalListingResponse r = new RentalListingResponse();
        r.setId(listing.getId());
        r.setOwnerId(listing.getOwnerId());
        r.setStatus(listing.getStatus());
        r.setCreatedAt(listing.getCreatedAt());
        r.setUpdatedAt(listing.getUpdatedAt());
        r.setExpiresAt(listing.getExpiresAt());
        mapEditFields(listing, r);
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
        r.setAvailableFrom(listing.getAvailableFrom());
        r.setAvailableAsap(listing.isAvailableAsap());
        r.setBedrooms(listing.getBedrooms());
        r.setBathrooms(listing.getBathrooms());
        r.setSharedBedroom(listing.getSharedBedroom());
        r.setSharedBathroom(listing.getSharedBathroom());
        r.setOwner(listing.getOwner());
        r.setPetPolicy(listing.getPetPolicy());
        r.setSmokingPolicy(listing.getSmokingPolicy());

        r.setIncludedUtilities(listing.getIncludedUtilities());
        r.setExtraServices(listing.getExtraServices());
        r.setLeaseTerms(listing.getLeaseTerms());
        r.setNearbyAmenities(listing.getNearbyAmenities());
        r.setPropertyAmenities(listing.getPropertyAmenities());

        r.setAdditionalDetails(listing.getAdditionalDetails());
        r.setTitle(listing.getTitle());
        r.setDescription(listing.getDescription());

        if (listing.getPromotionState() != null) {
            r.setPromotionType(listing.getPromotionState().getPromotionType());
            r.setPromotionPriority(listing.getPromotionState().getPromotionPriority());
            r.setPromotedUntil(listing.getPromotionState().getPromotedUntil());
        }

        // Read only photo id + main flag; loading listing.getPhotos() would pull every image
        // MEDIUMBLOB into heap and exhaust memory on list endpoints.
        List<ListingPhotoResponse> photos = photoRepository.findMetaByListingId(listing.getId()).stream()
                .map(p -> new ListingPhotoResponse(p.getId(), buildPhotoUrl(p.getId()), p.isMain()))
                .collect(Collectors.toList());
        r.setPhotos(photos);
        photos.stream().filter(ListingPhotoResponse::isMain).findFirst()
                .ifPresent(main -> r.setMainPhotoUrl(main.getUrl()));
        if (r.getMainPhotoUrl() == null && !photos.isEmpty()) {
            r.setMainPhotoUrl(photos.get(0).getUrl());
        }

        if (user != null) {
            r.setMatchScore(matchScorer.score(user, listing));

            // Rentals do not model "furnished" or room amenities; pets are derived from the pet policy.
            Boolean petsAllowed = listing.getPetPolicy() != null
                    ? listing.getPetPolicy() != PetPolicy.NOT_ALLOWED
                    : null;
            r.setPreferenceMatch(PreferenceMatcher.evaluate(user.getPreferences(),
                    listing.getRent(), listing.getPropertyType(), null, petsAllowed,
                    listing.getBedrooms(), listing.getBathrooms(), listing.getCity(),
                    listing.getCountry(), null, listing.getNearbyAmenities()));
        }
        return r;
    }

    private static void mapEditFields(RentalListing listing, RentalListingResponse r) {
        r.setEditCount(listing.getOwnerEditCount());
        r.setMaxEdits(ListingOwnerEditPolicy.MAX_OWNER_EDITS);
        r.setRemainingEdits(ListingOwnerEditPolicy.remainingEdits(listing.getOwnerEditCount()));
        r.setRejectionReason(listing.getRejectionReason());
    }

    public String buildPhotoUrl(Long photoId) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/rental-listings/photos/")
                .path(String.valueOf(photoId))
                .toUriString();
    }

    public void enrichOwner(RentalListingResponse response, Long ownerId) {
        if (ownerId == null) {
            return;
        }
        // Use a blob-free summary (id/name/hasPhoto) instead of loading the full owner entity:
        // loading the owner per card would pull the profile-photo MEDIUMBLOB into heap for every
        // listing in a page and exhaust memory under load.
        UserParticipantSummary owner = userService.getUserSummary(ownerId);
        if (owner == null) {
            return;
        }
        response.setOwnerName(owner.getName());
        if (owner.getHasPhoto()) {
            String photoUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/user/")
                    .path(String.valueOf(ownerId))
                    .path("/picture")
                    .toUriString();
            response.setOwnerPhotoUrl(photoUrl);
        }
    }
}
