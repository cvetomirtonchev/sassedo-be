package server.sassedo.listing.rental.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import server.sassedo.listing.common.ListingFilter;
import server.sassedo.listing.common.ListingStatus;
import server.sassedo.listing.rental.data.dto.RentalListing;
import server.sassedo.listing.rental.data.dto.RentalListingPhoto;
import server.sassedo.listing.rental.data.network.request.RentalListingRequest;
import server.sassedo.model.GenericException;

import java.io.IOException;
import java.util.List;

public interface RentalListingService {

    RentalListing create(Long ownerId, RentalListingRequest request) throws GenericException;

    RentalListing getById(Long id) throws GenericException;

    RentalListing getActiveById(Long id) throws GenericException;

    RentalListing getViewableById(Long id, Long requesterId, boolean admin) throws GenericException;

    Page<RentalListing> browse(ListingFilter filter, Pageable pageable);

    List<RentalListing> browseAllForMatch(ListingFilter filter);

    List<RentalListing> randomActive(int limit);

    Page<RentalListing> adminSearch(ListingStatus status, String search, Pageable pageable);

    List<RentalListing> getMyListings(Long ownerId);

    RentalListing update(Long id, Long ownerId, boolean admin, RentalListingRequest request) throws GenericException;

    RentalListing setStatus(Long id, ListingStatus status) throws GenericException;

    RentalListing renew(Long id, Long ownerId) throws GenericException;

    RentalListing deactivate(Long id, Long ownerId) throws GenericException;

    RentalListing reactivate(Long id, Long ownerId) throws GenericException;

    void delete(Long id) throws GenericException;

    RentalListing addPhotos(Long listingId, Long ownerId, MultipartFile[] files, Integer mainIndex) throws GenericException, IOException;

    RentalListingPhoto getPhoto(Long photoId) throws GenericException;

    RentalListingPhoto getMainPhoto(Long listingId) throws GenericException;
}
