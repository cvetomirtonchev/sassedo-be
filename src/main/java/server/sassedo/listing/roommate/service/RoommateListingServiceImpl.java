package server.sassedo.listing.roommate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import server.sassedo.listing.common.ListingStatus;
import server.sassedo.listing.common.PropertyType;
import server.sassedo.listing.roommate.data.dto.RoommateListing;
import server.sassedo.listing.roommate.data.dto.RoommateListingPhoto;
import server.sassedo.listing.roommate.data.network.request.RoommateListingRequest;
import server.sassedo.listing.roommate.repository.RoommateListingPhotoRepository;
import server.sassedo.listing.roommate.repository.RoommateListingRepository;
import server.sassedo.location.data.dto.City;
import server.sassedo.location.data.dto.Country;
import server.sassedo.location.repository.CityRepository;
import server.sassedo.location.repository.CountryRepository;
import server.sassedo.model.GenericException;
import server.sassedo.model.GenericExceptionCode;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoommateListingServiceImpl implements RoommateListingService {

    private final RoommateListingRepository listingRepository;
    private final RoommateListingPhotoRepository photoRepository;
    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;

    @Override
    @Transactional
    public RoommateListing create(Long ownerId, RoommateListingRequest request) throws GenericException {
        if (ownerId == null) {
            throw new GenericException(GenericExceptionCode.USER_NOT_FOUND, "User not found");
        }
        RoommateListing listing = new RoommateListing();
        listing.setOwnerId(ownerId);
        listing.setStatus(ListingStatus.PENDING);
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
    public Page<RoommateListing> browse(Long cityId, PropertyType propertyType, Pageable pageable) {
        return listingRepository.browse(ListingStatus.ACTIVE, cityId, propertyType, pageable);
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
        // A user editing their own listing resets it to pending review; admins keep the current status.
        if (!admin) {
            listing.setStatus(ListingStatus.PENDING);
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
        return listingRepository.save(listing);
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
            RoommateListingPhoto photo = new RoommateListingPhoto();
            photo.setData(file.getBytes());
            photo.setContentType(file.getContentType());
            photo.setListing(listing);
            boolean isMain = (mainIndex != null && mainIndex == i) || (!hasMain && i == 0 && mainIndex == null);
            if (isMain) {
                listing.getPhotos().forEach(p -> p.setMain(false));
                hasMain = true;
            }
            photo.setMain(isMain);
            listing.getPhotos().add(photo);
        }
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
        listing.setAboutMe(request.getAboutMe());

        listing.setTitle(request.getTitle());
        listing.setDescription(request.getDescription());
    }
}
