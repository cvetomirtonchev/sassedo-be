package server.sassedo.listing.roommate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import server.sassedo.listing.roommate.data.dto.RoommateListing;
import server.sassedo.listing.roommate.data.network.response.ListingPhotoResponse;
import server.sassedo.listing.roommate.data.network.response.RoommateListingResponse;
import server.sassedo.listing.roommate.matching.RoommateMatchScorer;
import server.sassedo.model.GenericException;
import server.sassedo.user.data.dto.User;
import server.sassedo.user.service.user.UserService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Entity -> response mapping for roommate listings (repo convention: manual mapping, no MapStruct).
 * Extracted from the controller so the same mapping can be reused by the favorites endpoints.
 */
@Component
@RequiredArgsConstructor
public class RoommateListingMapper {

    private final UserService userService;
    private final RoommateMatchScorer matchScorer;

    public RoommateListingResponse map(RoommateListing listing) {
        return map(listing, null);
    }

    public RoommateListingResponse map(RoommateListing listing, User user) {
        RoommateListingResponse r = new RoommateListingResponse();
        r.setId(listing.getId());
        r.setOwnerId(listing.getOwnerId());
        r.setStatus(listing.getStatus());
        r.setCreatedAt(listing.getCreatedAt());
        r.setUpdatedAt(listing.getUpdatedAt());
        r.setExpiresAt(listing.getExpiresAt());
        // Treat a null column (rows predating this feature) as "has property".
        r.setHasProperty(listing.getHasProperty() == null || listing.getHasProperty());
        r.setBudget(listing.getBudget());
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
        r.setAreaSqm(listing.getAreaSqm());
        r.setRoomArrangement(listing.getRoomArrangement());
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
        r.setHasChildren(listing.getHasChildren());
        r.setAboutMe(listing.getAboutMe());

        r.setTitle(listing.getTitle());
        r.setDescription(listing.getDescription());

        if (listing.getPromotionState() != null) {
            r.setPromotionType(listing.getPromotionState().getPromotionType());
            r.setPromotionPriority(listing.getPromotionState().getPromotionPriority());
            r.setPromotedUntil(listing.getPromotionState().getPromotedUntil());
            r.setPinned(listing.getPromotionState().isPinned());
        }

        List<ListingPhotoResponse> photos = listing.getPhotos().stream()
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
        }
        return r;
    }

    public String buildPhotoUrl(Long photoId) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/roommate-listings/photos/")
                .path(String.valueOf(photoId))
                .toUriString();
    }

    public void enrichOwner(RoommateListingResponse response, Long ownerId) {
        if (ownerId == null) {
            return;
        }
        try {
            User owner = userService.getUserById(ownerId);
            response.setOwnerName(owner.getName());
            if (owner.getProfilePhoto() != null && owner.getProfilePhoto().length > 0) {
                String photoUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/user/")
                        .path(String.valueOf(ownerId))
                        .path("/picture")
                        .toUriString();
                response.setOwnerPhotoUrl(photoUrl);
            }
        } catch (GenericException ignored) {
            // owner missing; leave owner fields null
        }
    }
}
