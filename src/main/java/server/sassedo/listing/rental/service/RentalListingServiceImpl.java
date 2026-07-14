package server.sassedo.listing.rental.service;

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
import server.sassedo.listing.rental.data.dto.RentalListing;
import server.sassedo.listing.rental.data.dto.RentalListingPhoto;
import server.sassedo.listing.rental.data.network.request.RentalListingRequest;
import server.sassedo.listing.rental.repository.RentalListingPhotoRepository;
import server.sassedo.listing.rental.repository.RentalListingRepository;
import server.sassedo.listing.rental.repository.RentalListingSpecifications;
import server.sassedo.location.data.dto.City;
import server.sassedo.location.data.dto.Country;
import server.sassedo.location.repository.CityRepository;
import server.sassedo.location.repository.CountryRepository;
import server.sassedo.model.GenericException;
import server.sassedo.model.GenericExceptionCode;
import server.sassedo.utils.ImageUploadValidator;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RentalListingServiceImpl implements RentalListingService {

    private static final int MATCH_BROWSE_CAP = 1000;

    private final RentalListingRepository listingRepository;
    private final RentalListingPhotoRepository photoRepository;
    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;

    @Value("${sassedo.listings.ttl-days:30}")
    private long listingTtlDays;

    @Override
    @Transactional
    public RentalListing create(Long ownerId, RentalListingRequest request) throws GenericException {
        if (ownerId == null) {
            throw new GenericException(GenericExceptionCode.USER_NOT_FOUND, "User not found");
        }
        RentalListing listing = new RentalListing();
        listing.setOwnerId(ownerId);
        listing.setStatus(ListingStatus.PENDING);
        applyRequest(listing, request);
        return listingRepository.save(listing);
    }

    @Override
    public RentalListing getById(Long id) throws GenericException {
        return listingRepository.findById(id)
                .orElseThrow(() -> new GenericException(GenericExceptionCode.LISTING_NOT_FOUND, "Listing not found"));
    }

    @Override
    public RentalListing getActiveById(Long id) throws GenericException {
        RentalListing listing = getById(id);
        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new GenericException(GenericExceptionCode.LISTING_NOT_FOUND, "Listing not found");
        }
        return listing;
    }

    @Override
    public RentalListing getViewableById(Long id, Long requesterId, boolean admin) throws GenericException {
        RentalListing listing = getById(id);
        boolean owner = requesterId != null && requesterId.equals(listing.getOwnerId());
        if (listing.getStatus() != ListingStatus.ACTIVE && !admin && !owner) {
            throw new GenericException(GenericExceptionCode.LISTING_NOT_FOUND, "Listing not found");
        }
        return listing;
    }

    @Override
    public Page<RentalListing> browse(ListingFilter filter, Pageable pageable) {
        return listingRepository.findAll(RentalListingSpecifications.browse(filter), pageable);
    }

    @Override
    public List<RentalListing> browseAllForMatch(ListingFilter filter) {
        // Match ranking is computed in-memory, so we load all filtered ACTIVE listings up to a
        // sensible cap. Fine for the expected catalog size; revisit if it grows very large.
        return listingRepository.findAll(RentalListingSpecifications.browse(filter),
                PageRequest.of(0, MATCH_BROWSE_CAP)).getContent();
    }

    @Override
    public List<RentalListing> randomActive(int limit) {
        return listingRepository.findRandomActive(limit);
    }

    @Override
    public Page<RentalListing> adminSearch(ListingStatus status, String search, Pageable pageable) {
        return listingRepository.adminSearch(status, search, pageable);
    }

    @Override
    public List<RentalListing> getMyListings(Long ownerId) {
        return listingRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId);
    }

    @Override
    @Transactional
    public RentalListing update(Long id, Long ownerId, boolean admin, RentalListingRequest request) throws GenericException {
        RentalListing listing = getById(id);
        if (!admin && (ownerId == null || !ownerId.equals(listing.getOwnerId()))) {
            throw new GenericException(GenericExceptionCode.NOT_LISTING_OWNER, "You are not the owner of this listing");
        }
        applyRequest(listing, request);
        if (!admin) {
            listing.setStatus(ListingStatus.PENDING);
        }
        return listingRepository.save(listing);
    }

    @Override
    @Transactional
    public RentalListing setStatus(Long id, ListingStatus status) throws GenericException {
        if (status == null) {
            throw new GenericException(GenericExceptionCode.INVALID_LISTING_STATUS, "Invalid listing status");
        }
        RentalListing listing = getById(id);
        listing.setStatus(status);
        if (status == ListingStatus.ACTIVE) {
            listing.setExpiresAt(LocalDateTime.now().plusDays(listingTtlDays));
        }
        return listingRepository.save(listing);
    }

    @Override
    @Transactional
    public RentalListing renew(Long id, Long ownerId) throws GenericException {
        RentalListing listing = requireOwner(id, ownerId);
        listing.setExpiresAt(LocalDateTime.now().plusDays(listingTtlDays));
        if (listing.getStatus() == ListingStatus.EXPIRED) {
            listing.setStatus(ListingStatus.ACTIVE);
        }
        return listingRepository.save(listing);
    }

    @Override
    @Transactional
    public RentalListing deactivate(Long id, Long ownerId) throws GenericException {
        RentalListing listing = requireOwner(id, ownerId);
        listing.setStatus(ListingStatus.INACTIVE);
        return listingRepository.save(listing);
    }

    @Override
    @Transactional
    public RentalListing reactivate(Long id, Long ownerId) throws GenericException {
        RentalListing listing = requireOwner(id, ownerId);
        listing.setStatus(ListingStatus.ACTIVE);
        listing.setExpiresAt(LocalDateTime.now().plusDays(listingTtlDays));
        return listingRepository.save(listing);
    }

    private RentalListing requireOwner(Long id, Long ownerId) throws GenericException {
        RentalListing listing = getById(id);
        if (ownerId == null || !ownerId.equals(listing.getOwnerId())) {
            throw new GenericException(GenericExceptionCode.NOT_LISTING_OWNER, "You are not the owner of this listing");
        }
        return listing;
    }

    @Override
    @Transactional
    public void delete(Long id) throws GenericException {
        RentalListing listing = getById(id);
        listingRepository.delete(listing);
    }

    @Override
    @Transactional
    public RentalListing addPhotos(Long listingId, Long ownerId, MultipartFile[] files, Integer mainIndex) throws GenericException, IOException {
        RentalListing listing = getById(listingId);
        if (ownerId == null || !ownerId.equals(listing.getOwnerId())) {
            throw new GenericException(GenericExceptionCode.NOT_LISTING_OWNER, "You are not the owner of this listing");
        }
        if (files == null || files.length == 0) {
            throw new GenericException(GenericExceptionCode.INVALID_FILE, "No files provided");
        }
        boolean hasMain = listing.getPhotos().stream().anyMatch(RentalListingPhoto::isMain);
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            if (file == null || file.isEmpty()) {
                continue;
            }
            ImageUploadValidator.validate(file);
            RentalListingPhoto photo = new RentalListingPhoto();
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
    public RentalListingPhoto getPhoto(Long photoId) throws GenericException {
        return photoRepository.findById(photoId)
                .orElseThrow(() -> new GenericException(GenericExceptionCode.LISTING_PHOTO_NOT_FOUND, "Photo not found"));
    }

    @Override
    public RentalListingPhoto getMainPhoto(Long listingId) throws GenericException {
        RentalListingPhoto main = photoRepository.findFirstByListingIdAndMainTrue(listingId);
        if (main != null) {
            return main;
        }
        RentalListingPhoto first = photoRepository.findFirstByListingIdOrderByIdAsc(listingId);
        if (first == null) {
            throw new GenericException(GenericExceptionCode.LISTING_PHOTO_NOT_FOUND, "Photo not found");
        }
        return first;
    }

    private void applyRequest(RentalListing listing, RentalListingRequest request) throws GenericException {
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
        listing.setAvailableFrom(request.isAvailableAsap() ? null : request.getAvailableFrom());
        listing.setAvailableAsap(request.isAvailableAsap());
        listing.setBedrooms(request.getBedrooms());
        listing.setBathrooms(request.getBathrooms());
        listing.setSharedBedroom(request.getSharedBedroom());
        listing.setSharedBathroom(request.getSharedBathroom());
        listing.setOwner(request.getOwner());
        listing.setPetPolicy(request.getPetPolicy());
        listing.setSmokingPolicy(request.getSmokingPolicy());

        listing.getIncludedUtilities().clear();
        if (request.getIncludedUtilities() != null) {
            listing.getIncludedUtilities().addAll(request.getIncludedUtilities());
        }
        listing.getExtraServices().clear();
        if (request.getExtraServices() != null) {
            listing.getExtraServices().addAll(request.getExtraServices());
        }
        listing.getLeaseTerms().clear();
        if (request.getLeaseTerms() != null) {
            listing.getLeaseTerms().addAll(request.getLeaseTerms());
        }
        listing.getNearbyAmenities().clear();
        if (request.getNearbyAmenities() != null) {
            listing.getNearbyAmenities().addAll(request.getNearbyAmenities());
        }
        listing.getPropertyAmenities().clear();
        if (request.getPropertyAmenities() != null) {
            listing.getPropertyAmenities().addAll(request.getPropertyAmenities());
        }

        listing.setAdditionalDetails(request.getAdditionalDetails());
        listing.setTitle(request.getTitle());
        listing.setDescription(request.getDescription());
    }
}
