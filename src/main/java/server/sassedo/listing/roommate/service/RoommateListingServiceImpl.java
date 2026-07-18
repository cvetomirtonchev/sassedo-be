package server.sassedo.listing.roommate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import server.sassedo.listing.common.ListingFilter;
import server.sassedo.listing.common.ListingStatus;
import server.sassedo.listing.roommate.data.dto.RoommateListing;
import server.sassedo.listing.roommate.data.dto.RoommateListingPhoto;
import server.sassedo.listing.roommate.data.network.request.RoommateListingRequest;
import server.sassedo.listing.roommate.repository.RoommateListingPhotoRepository;
import server.sassedo.listing.roommate.repository.RoommateListingRepository;
import server.sassedo.listing.roommate.repository.RoommateListingSpecifications;
import server.sassedo.location.data.dto.City;
import server.sassedo.location.data.dto.Country;
import server.sassedo.location.repository.CityRepository;
import server.sassedo.location.repository.CountryRepository;
import server.sassedo.model.GenericException;
import server.sassedo.model.GenericExceptionCode;
import server.sassedo.moderation.risk.domain.ImageFingerprint;
import server.sassedo.moderation.risk.domain.ListingSnapshot;
import server.sassedo.moderation.risk.domain.RiskAssessment;
import server.sassedo.moderation.risk.service.ListingFingerprintService;
import server.sassedo.moderation.risk.service.ListingSnapshotFactory;
import server.sassedo.moderation.risk.service.PhotoHasher;
import server.sassedo.moderation.risk.service.RiskSubmissionService;
import server.sassedo.promotion.common.ListingType;
import server.sassedo.user.data.dto.User;
import server.sassedo.user.service.user.UserService;
import server.sassedo.utils.ImageUploadValidator;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoommateListingServiceImpl implements RoommateListingService {

    private static final int MATCH_BROWSE_CAP = 1000;

    private final RoommateListingRepository listingRepository;
    private final RoommateListingPhotoRepository photoRepository;
    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;
    private final ListingSnapshotFactory snapshotFactory;
    private final RiskSubmissionService riskSubmissionService;
    private final ListingFingerprintService fingerprintService;
    private final UserService userService;

    @Value("${sassedo.listings.ttl-days:30}")
    private long listingTtlDays;

    @Override
    @Transactional
    public RoommateListing create(Long ownerId, RoommateListingRequest request) throws GenericException {
        if (ownerId == null) {
            throw new GenericException(GenericExceptionCode.USER_NOT_FOUND, "User not found");
        }
        RoommateListing listing = new RoommateListing();
        listing.setOwnerId(ownerId);
        // New listings start as drafts; publication happens only through submit() after risk scoring.
        listing.setStatus(ListingStatus.DRAFT);
        applyRequest(listing, request);
        return listingRepository.save(listing);
    }

    @Override
    public RoommateListing getById(Long id) throws GenericException {
        return listingRepository.findById(id)
                .orElseThrow(() -> new GenericException(GenericExceptionCode.LISTING_NOT_FOUND, "Listing not found"));
    }

    @Override
    public RoommateListing getActiveById(Long id) throws GenericException {
        RoommateListing listing = getById(id);
        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new GenericException(GenericExceptionCode.LISTING_NOT_FOUND, "Listing not found");
        }
        return listing;
    }

    @Override
    public RoommateListing getViewableById(Long id, Long requesterId, boolean admin) throws GenericException {
        RoommateListing listing = getById(id);
        boolean owner = requesterId != null && requesterId.equals(listing.getOwnerId());
        if (listing.getStatus() != ListingStatus.ACTIVE && !admin && !owner) {
            throw new GenericException(GenericExceptionCode.LISTING_NOT_FOUND, "Listing not found");
        }
        return listing;
    }

    @Override
    public Page<RoommateListing> browse(ListingFilter filter, Pageable pageable) {
        return listingRepository.findAll(RoommateListingSpecifications.browse(filter), pageable);
    }

    @Override
    public List<RoommateListing> browseAllForMatch(ListingFilter filter) {
        // Match ranking is computed in-memory, so we load all filtered ACTIVE listings up to a
        // sensible cap. Fine for the expected catalog size; revisit if it grows very large.
        return listingRepository.findAll(RoommateListingSpecifications.browse(filter),
                PageRequest.of(0, MATCH_BROWSE_CAP)).getContent();
    }

    @Override
    public List<RoommateListing> randomActive(int limit) {
        return listingRepository.findRandomActive(limit);
    }

    @Override
    public Page<RoommateListing> adminSearch(ListingStatus status, String search, Pageable pageable) {
        return listingRepository.adminSearch(status, search, pageable);
    }

    @Override
    public List<RoommateListing> getMyListings(Long ownerId) {
        return listingRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId);
    }

    @Override
    @Transactional
    public RoommateListing update(Long id, Long ownerId, boolean admin, RoommateListingRequest request) throws GenericException {
        RoommateListing listing = getById(id);
        if (!admin && (ownerId == null || !ownerId.equals(listing.getOwnerId()))) {
            throw new GenericException(GenericExceptionCode.NOT_LISTING_OWNER, "You are not the owner of this listing");
        }
        applyRequest(listing, request);
        // Editing invalidates any prior approval: the listing returns to draft and must be
        // re-submitted so the risk engine re-scores the new content. Admins keep the current status.
        if (!admin) {
            listing.setStatus(ListingStatus.DRAFT);
        }
        return listingRepository.save(listing);
    }

    @Override
    @Transactional
    public RoommateListing setStatus(Long id, ListingStatus status) throws GenericException {
        if (status == null) {
            throw new GenericException(GenericExceptionCode.INVALID_LISTING_STATUS, "Invalid listing status");
        }
        RoommateListing listing = getById(id);
        listing.setStatus(status);
        if (status == ListingStatus.ACTIVE) {
            listing.setExpiresAt(LocalDateTime.now().plusDays(listingTtlDays));
        }
        RoommateListing saved = listingRepository.save(listing);
        fingerprintService.updateStatus(ListingType.ROOMMATE, saved.getId(), status);
        return saved;
    }

    @Override
    @Transactional
    public RiskAssessment submit(Long id, Long ownerId, String clientIp) throws GenericException {
        RoommateListing listing = requireOwner(id, ownerId);
        return evaluateAndApply(listing, clientIp);
    }

    private RiskAssessment evaluateAndApply(RoommateListing listing, String clientIp) throws GenericException {
        User owner = userService.getUserById(listing.getOwnerId());
        ListingSnapshot snapshot = snapshotFactory.roommate(listing);
        List<ImageFingerprint> images = PhotoHasher.fromRows(photoRepository.findHashRowsByListingId(listing.getId()));
        RiskAssessment assessment = riskSubmissionService.evaluate(
                snapshot, owner, images, clientIp, listing.getStatus());
        ListingStatus finalStatus = assessment.decision().resultingStatus();
        listing.setStatus(finalStatus);
        if (finalStatus == ListingStatus.ACTIVE) {
            listing.setExpiresAt(LocalDateTime.now().plusDays(listingTtlDays));
        }
        listingRepository.save(listing);
        return assessment;
    }

    @Override
    @Transactional
    public RoommateListing renew(Long id, Long ownerId) throws GenericException {
        RoommateListing listing = requireOwner(id, ownerId);
        listing.setExpiresAt(LocalDateTime.now().plusDays(listingTtlDays));
        if (listing.getStatus() == ListingStatus.EXPIRED) {
            listing.setStatus(ListingStatus.ACTIVE);
        }
        return listingRepository.save(listing);
    }

    @Override
    @Transactional
    public RoommateListing deactivate(Long id, Long ownerId) throws GenericException {
        RoommateListing listing = requireOwner(id, ownerId);
        listing.setStatus(ListingStatus.INACTIVE);
        RoommateListing saved = listingRepository.save(listing);
        fingerprintService.updateStatus(ListingType.ROOMMATE, saved.getId(), ListingStatus.INACTIVE);
        return saved;
    }

    @Override
    @Transactional
    public RoommateListing reactivate(Long id, Long ownerId) throws GenericException {
        RoommateListing listing = requireOwner(id, ownerId);
        // Reactivation is a re-publication, so it must pass through the risk engine.
        evaluateAndApply(listing, null);
        return listing;
    }

    private RoommateListing requireOwner(Long id, Long ownerId) throws GenericException {
        RoommateListing listing = getById(id);
        if (ownerId == null || !ownerId.equals(listing.getOwnerId())) {
            throw new GenericException(GenericExceptionCode.NOT_LISTING_OWNER, "You are not the owner of this listing");
        }
        return listing;
    }

    @Override
    @Transactional
    public void delete(Long id) throws GenericException {
        RoommateListing listing = getById(id);
        listingRepository.delete(listing);
    }

    @Override
    @Transactional
    public RoommateListing addPhotos(Long listingId, Long ownerId, MultipartFile[] files, Integer mainIndex) throws GenericException, IOException {
        RoommateListing listing = getById(listingId);
        if (ownerId == null || !ownerId.equals(listing.getOwnerId())) {
            throw new GenericException(GenericExceptionCode.NOT_LISTING_OWNER, "You are not the owner of this listing");
        }
        if (files == null || files.length == 0) {
            throw new GenericException(GenericExceptionCode.INVALID_FILE, "No files provided");
        }
        boolean hasMain = listing.getPhotos().stream().anyMatch(RoommateListingPhoto::isMain);
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            if (file == null || file.isEmpty()) {
                continue;
            }
            ImageUploadValidator.validate(file);
            RoommateListingPhoto photo = new RoommateListingPhoto();
            byte[] bytes = file.getBytes();
            photo.setData(bytes);
            photo.setContentType(file.getContentType());
            // Hash once at upload so submission-time duplicate checks never touch image BLOBs.
            photo.setSha256(PhotoHasher.sha256(bytes));
            photo.setDhash(PhotoHasher.dHash(bytes));
            photo.setListing(listing);
            boolean isMain = (mainIndex != null && mainIndex == i) || (!hasMain && i == 0 && mainIndex == null);
            if (isMain) {
                listing.getPhotos().forEach(p -> p.setMain(false));
                hasMain = true;
            }
            photo.setMain(isMain);
            listing.getPhotos().add(photo);
        }
        // Changing photos invalidates any prior approval; the owner must re-submit.
        listing.setStatus(ListingStatus.DRAFT);
        listing.setContentRevision(listing.getContentRevision() + 1);
        return listingRepository.save(listing);
    }

    @Override
    public RoommateListingPhoto getPhoto(Long photoId) throws GenericException {
        return photoRepository.findById(photoId)
                .orElseThrow(() -> new GenericException(GenericExceptionCode.LISTING_PHOTO_NOT_FOUND, "Photo not found"));
    }

    @Override
    public RoommateListingPhoto getMainPhoto(Long listingId) throws GenericException {
        RoommateListingPhoto main = photoRepository.findFirstByListingIdAndMainTrue(listingId);
        if (main != null) {
            return main;
        }
        RoommateListingPhoto first = photoRepository.findFirstByListingIdOrderByIdAsc(listingId);
        if (first == null) {
            throw new GenericException(GenericExceptionCode.LISTING_PHOTO_NOT_FOUND, "Photo not found");
        }
        return first;
    }

    private void applyRequest(RoommateListing listing, RoommateListingRequest request) throws GenericException {
        // Bump the content revision on every edit so the risk engine can detect concurrent changes.
        listing.setContentRevision(listing.getContentRevision() + 1);
        // Default to "has property" when the client omits the flag (backward compatibility).
        boolean hasProperty = request.getHasProperty() == null || request.getHasProperty();
        listing.setHasProperty(hasProperty);

        if (!hasProperty) {
            applyWithoutProperty(listing, request);
            return;
        }

        listing.setBudget(null);
        listing.setPropertyType(request.getPropertyType());

        if (request.getCountryId() != null) {
            Country country = countryRepository.findById(request.getCountryId())
                    .orElseThrow(() -> new GenericException(GenericExceptionCode.COUNTRY_NOT_FOUND, "Country not found"));
            listing.setCountry(country);
        } else {
            listing.setCountry(null);
        }
        if (request.getCityId() != null) {
            City city = cityRepository.findById(request.getCityId())
                    .orElseThrow(() -> new GenericException(GenericExceptionCode.CITY_NOT_FOUND, "City not found"));
            listing.setCity(city);
        } else {
            listing.setCity(null);
        }
        listing.setNeighborhood(request.getNeighborhood());
        listing.setAddress(request.getAddress());

        listing.setRent(request.getRent());
        listing.setCostsIncluded(request.getCostsIncluded());
        listing.setDeposit(request.getDeposit());
        listing.setRoomsCount(request.getRoomsCount());
        listing.setFurnished(request.getFurnished());
        listing.setPetsAllowed(request.getPetsAllowed());
        listing.setAvailableFrom(request.isAvailableAsap() ? null : request.getAvailableFrom());
        listing.setAvailableAsap(request.isAvailableAsap());
        listing.setBedrooms(request.getBedrooms());
        listing.setBathrooms(request.getBathrooms());
        listing.setAreaSqm(request.getAreaSqm());
        listing.setRoomArrangement(request.getRoomArrangement());
        listing.setOwner(request.getOwner());

        listing.getIncludedUtilities().clear();
        if (request.getIncludedUtilities() != null) {
            listing.getIncludedUtilities().addAll(request.getIncludedUtilities());
        }
        listing.getExtraServices().clear();
        if (request.getExtraServices() != null) {
            listing.getExtraServices().addAll(request.getExtraServices());
        }
        listing.getNearbyAmenities().clear();
        if (request.getNearbyAmenities() != null) {
            listing.getNearbyAmenities().addAll(request.getNearbyAmenities());
        }
        listing.getRoomAmenities().clear();
        if (request.getRoomAmenities() != null) {
            listing.getRoomAmenities().addAll(request.getRoomAmenities());
        }

        listing.setPreferredSex(request.getPreferredSex());
        listing.setPreferredOrientation(request.getPreferredOrientation());
        listing.setAgeMin(request.getAgeMin());
        listing.setAgeMax(request.getAgeMax());
        listing.setSmokingPreference(request.getSmokingPreference());
        listing.setOccupationPreference(request.getOccupationPreference());
        listing.setAdditionalRequirements(request.getAdditionalRequirements());
        listing.setPetPolicy(request.getPetPolicy());
        listing.setPeopleInProperty(request.getPeopleInProperty());
        listing.getSpokenLanguages().clear();
        if (request.getSpokenLanguages() != null) {
            listing.getSpokenLanguages().addAll(request.getSpokenLanguages());
        }
        listing.setEmploymentStatus(request.getEmploymentStatus());
        listing.setHasChildren(request.getHasChildren());
        listing.setAboutMe(request.getAboutMe());

        listing.setTitle(request.getTitle());
        listing.setDescription(request.getDescription());
    }

    // Applies a request for a lister without a property: keeps location, budget, roommate
    // requirements and title/description, but clears all property-specific fields so no stale
    // property data lingers on the listing.
    private void applyWithoutProperty(RoommateListing listing, RoommateListingRequest request) throws GenericException {
        listing.setBudget(request.getBudget());
        listing.setPropertyType(null);

        if (request.getCountryId() != null) {
            Country country = countryRepository.findById(request.getCountryId())
                    .orElseThrow(() -> new GenericException(GenericExceptionCode.COUNTRY_NOT_FOUND, "Country not found"));
            listing.setCountry(country);
        } else {
            listing.setCountry(null);
        }
        if (request.getCityId() != null) {
            City city = cityRepository.findById(request.getCityId())
                    .orElseThrow(() -> new GenericException(GenericExceptionCode.CITY_NOT_FOUND, "City not found"));
            listing.setCity(city);
        } else {
            listing.setCity(null);
        }
        listing.setNeighborhood(request.getNeighborhood());
        listing.setAddress(request.getAddress());

        listing.setRent(null);
        listing.setCostsIncluded(null);
        listing.setDeposit(null);
        listing.setRoomsCount(null);
        listing.setFurnished(null);
        listing.setPetsAllowed(null);
        listing.setAvailableFrom(request.isAvailableAsap() ? null : request.getAvailableFrom());
        listing.setAvailableAsap(request.isAvailableAsap());
        listing.setBedrooms(null);
        listing.setBathrooms(null);
        listing.setAreaSqm(null);
        listing.setRoomArrangement(null);
        listing.setOwner(null);

        listing.getIncludedUtilities().clear();
        listing.getExtraServices().clear();
        listing.getNearbyAmenities().clear();
        listing.getRoomAmenities().clear();

        listing.setPreferredSex(request.getPreferredSex());
        listing.setPreferredOrientation(request.getPreferredOrientation());
        listing.setAgeMin(request.getAgeMin());
        listing.setAgeMax(request.getAgeMax());
        listing.setSmokingPreference(request.getSmokingPreference());
        listing.setOccupationPreference(request.getOccupationPreference());
        listing.setAdditionalRequirements(request.getAdditionalRequirements());
        listing.setPetPolicy(request.getPetPolicy());
        listing.setPeopleInProperty(request.getPeopleInProperty());
        listing.getSpokenLanguages().clear();
        if (request.getSpokenLanguages() != null) {
            listing.getSpokenLanguages().addAll(request.getSpokenLanguages());
        }
        listing.setEmploymentStatus(request.getEmploymentStatus());
        listing.setHasChildren(request.getHasChildren());
        listing.setAboutMe(request.getAboutMe());

        listing.setTitle(request.getTitle());
        listing.setDescription(request.getDescription());
    }
}
