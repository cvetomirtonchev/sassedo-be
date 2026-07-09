package server.sassedo.listing.search.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import server.sassedo.listing.search.data.dto.ApartmentSearch;
import server.sassedo.listing.search.data.network.response.ApartmentSearchResponse;
import server.sassedo.model.GenericException;
import server.sassedo.user.data.dto.User;
import server.sassedo.user.service.user.UserService;

/**
 * Entity -> response mapping for apartment searches (repo convention: manual mapping, no MapStruct).
 * Extracted from the controller so the same mapping can be reused by the favorites endpoints.
 */
@Component
@RequiredArgsConstructor
public class ApartmentSearchMapper {

    private final UserService userService;

    public ApartmentSearchResponse map(ApartmentSearch entity) {
        ApartmentSearchResponse r = new ApartmentSearchResponse();
        r.setId(entity.getId());
        r.setOwnerId(entity.getOwnerId());
        r.setStatus(entity.getStatus());
        r.setCreatedAt(entity.getCreatedAt());
        r.setUpdatedAt(entity.getUpdatedAt());
        r.setExpiresAt(entity.getExpiresAt());
        r.setPropertyType(entity.getPropertyType());

        if (entity.getCountry() != null) {
            r.setCountryId(entity.getCountry().getId());
            r.setCountryNameEn(entity.getCountry().getNameEn());
            r.setCountryNameBg(entity.getCountry().getNameBg());
        }
        if (entity.getCity() != null) {
            r.setCityId(entity.getCity().getId());
            r.setCityNameEn(entity.getCity().getNameEn());
            r.setCityNameBg(entity.getCity().getNameBg());
        }
        r.setNeighborhood(entity.getNeighborhood());

        r.setBudgetMin(entity.getBudgetMin());
        r.setBudgetMax(entity.getBudgetMax());
        r.setAvailableFrom(entity.getAvailableFrom());
        r.setAvailableAsap(entity.isAvailableAsap());
        r.setLeaseTerms(entity.getLeaseTerms());

        r.setAge(entity.getAge());
        r.setSex(entity.getSex());
        r.setProfession(entity.getProfession());
        r.setSmoker(entity.getSmoker());
        r.setHasPets(entity.getHasPets());

        r.setTitle(entity.getTitle());
        r.setDescription(entity.getDescription());

        if (entity.getPromotionState() != null) {
            r.setPromotionType(entity.getPromotionState().getPromotionType());
            r.setPromotionPriority(entity.getPromotionState().getPromotionPriority());
            r.setPromotedUntil(entity.getPromotionState().getPromotedUntil());
            r.setPinned(entity.getPromotionState().isPinned());
        }
        return r;
    }

    public void enrichOwner(ApartmentSearchResponse response, Long ownerId) {
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
