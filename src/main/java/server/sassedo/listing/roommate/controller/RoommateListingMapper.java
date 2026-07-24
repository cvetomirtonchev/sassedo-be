package server.sassedo.listing.roommate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import server.sassedo.listing.common.ListingOwnerEditPolicy;
import server.sassedo.listing.common.matching.PreferenceMatcher;
import server.sassedo.listing.roommate.data.dto.RoommateListing;
import server.sassedo.listing.roommate.data.network.response.ListingPhotoResponse;
import server.sassedo.listing.roommate.data.network.response.OwnerProfileResponse;
import server.sassedo.listing.roommate.data.network.response.RoommateListingResponse;
import server.sassedo.listing.roommate.data.network.response.RoommateRequirementMatchResponse;
import server.sassedo.listing.roommate.matching.RoommateMatchResult;
import server.sassedo.listing.roommate.matching.RoommateMatchScorer;
import server.sassedo.listing.roommate.repository.RoommateListingPhotoRepository;
import server.sassedo.user.data.dto.User;
import server.sassedo.user.data.projection.PublicProfileView;
import server.sassedo.user.data.projection.UserParticipantSummary;
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
    private final RoommateListingPhotoRepository photoRepository;

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
        mapEditFields(listing, r);
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
        r.setPeopleInProperty(listing.getPeopleInProperty());
        r.setSharedBedroom(listing.getSharedBedroom());
        r.setSharedBathroom(listing.getSharedBathroom());
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
        r.setAdditionalRequirements(listing.getAdditionalRequirements());
        r.setPetPolicy(listing.getPetPolicy());
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
            RoommateMatchResult match = matchScorer.evaluate(user, listing);
            r.setMatchScore(match.getScore());
            RoommateRequirementMatchResponse requirementMatch = new RoommateRequirementMatchResponse();
            requirementMatch.setSex(match.getSex());
            requirementMatch.setAge(match.getAge());
            requirementMatch.setSmoking(match.getSmoking());
            requirementMatch.setPets(match.getPets());
            requirementMatch.setEmployment(match.getEmployment());
            requirementMatch.setLanguages(match.getLanguages());
            requirementMatch.setMatchedLanguages(match.getMatchedLanguages());
            r.setRequirementMatch(requirementMatch);

            r.setPreferenceMatch(PreferenceMatcher.evaluate(user.getPreferences(),
                    listing.getRent(), listing.getPropertyType(), listing.getFurnished(),
                    listing.getPetsAllowed(), listing.getBedrooms(), listing.getBathrooms(),
                    listing.getCity(), listing.getCountry(), listing.getRoomAmenities(),
                    listing.getNearbyAmenities()));
        }
        return r;
    }

    private static void mapEditFields(RoommateListing listing, RoommateListingResponse r) {
        r.setEditCount(listing.getOwnerEditCount());
        r.setMaxEdits(ListingOwnerEditPolicy.MAX_OWNER_EDITS);
        r.setRemainingEdits(ListingOwnerEditPolicy.remainingEdits(listing.getOwnerEditCount()));
        r.setRejectionReason(listing.getRejectionReason());
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

    /**
     * Attaches the owner's publicly shareable profile (age, gender, pets, smoking, languages,
     * employment, bio) to the detail response. Intended for the single-listing endpoint only, so
     * list/card responses do not incur the extra lookups. Sets {@code ownerName}/{@code
     * ownerPhotoUrl} as a fallback when they were not already enriched.
     */
    public void enrichOwnerProfile(RoommateListingResponse response, Long ownerId) {
        if (ownerId == null) {
            return;
        }
        PublicProfileView profile = userService.getPublicProfile(ownerId);
        if (profile == null) {
            return;
        }
        OwnerProfileResponse ownerProfile = new OwnerProfileResponse();
        ownerProfile.setAge(profile.getAge());
        ownerProfile.setSex(profile.getSex());
        ownerProfile.setPetPolicy(profile.getPetPolicy());
        ownerProfile.setSmokingPreference(profile.getSmokingPreference());
        ownerProfile.setEmploymentStatus(profile.getOccupation());
        ownerProfile.setAboutMe(profile.getShortDescription());
        ownerProfile.setLanguages(userService.getUserLanguages(ownerId));
        response.setOwnerProfile(ownerProfile);

        if (response.getOwnerName() == null) {
            response.setOwnerName(profile.getName());
        }
        if (response.getOwnerPhotoUrl() == null && profile.getHasPhoto()) {
            String photoUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/user/")
                    .path(String.valueOf(ownerId))
                    .path("/picture")
                    .toUriString();
            response.setOwnerPhotoUrl(photoUrl);
        }
    }
}
