package server.sassedo.listing.roommate.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import server.sassedo.listing.common.ListingFilter;
import server.sassedo.listing.common.ListingStatus;
import server.sassedo.listing.roommate.data.dto.RoommateListing;
import server.sassedo.listing.roommate.data.dto.RoommateListingPhoto;
import server.sassedo.listing.roommate.data.network.request.RoommateListingRequest;
import server.sassedo.model.GenericException;

import java.io.IOException;
import java.util.List;

public interface RoommateListingService {

    RoommateListing create(Long ownerId, RoommateListingRequest request) throws GenericException;

    RoommateListing getById(Long id) throws GenericException;

    RoommateListing getActiveById(Long id) throws GenericException;

    RoommateListing getViewableById(Long id, Long requesterId, boolean admin) throws GenericException;

    Page<RoommateListing> browse(ListingFilter filter, Pageable pageable);

    List<RoommateListing> browseAllForMatch(ListingFilter filter);

    Page<RoommateListing> adminSearch(ListingStatus status, String search, Pageable pageable);

    List<RoommateListing> getMyListings(Long ownerId);

    RoommateListing update(Long id, Long ownerId, boolean admin, RoommateListingRequest request) throws GenericException;

    RoommateListing setStatus(Long id, ListingStatus status) throws GenericException;

    RoommateListing renew(Long id, Long ownerId) throws GenericException;

    RoommateListing deactivate(Long id, Long ownerId) throws GenericException;

    RoommateListing reactivate(Long id, Long ownerId) throws GenericException;

    void delete(Long id) throws GenericException;

    RoommateListing addPhotos(Long listingId, Long ownerId, MultipartFile[] files, Integer mainIndex) throws GenericException, IOException;

    RoommateListingPhoto getPhoto(Long photoId) throws GenericException;

    RoommateListingPhoto getMainPhoto(Long listingId) throws GenericException;
}
