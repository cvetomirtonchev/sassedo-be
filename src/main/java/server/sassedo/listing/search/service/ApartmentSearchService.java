package server.sassedo.listing.search.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import server.sassedo.listing.common.ListingFilter;
import server.sassedo.listing.common.ListingStatus;
import server.sassedo.listing.search.data.dto.ApartmentSearch;
import server.sassedo.listing.search.data.network.request.ApartmentSearchRequest;
import server.sassedo.model.GenericException;

import java.util.List;

public interface ApartmentSearchService {

    ApartmentSearch create(Long ownerId, ApartmentSearchRequest request) throws GenericException;

    ApartmentSearch getById(Long id) throws GenericException;

    ApartmentSearch getActiveById(Long id) throws GenericException;

    ApartmentSearch getViewableById(Long id, Long requesterId, boolean admin) throws GenericException;

    Page<ApartmentSearch> browse(ListingFilter filter, Pageable pageable);

    Page<ApartmentSearch> adminSearch(ListingStatus status, String search, Pageable pageable);

    List<ApartmentSearch> getMyListings(Long ownerId);

    ApartmentSearch update(Long id, Long ownerId, boolean admin, ApartmentSearchRequest request) throws GenericException;

    ApartmentSearch setStatus(Long id, ListingStatus status) throws GenericException;

    ApartmentSearch renew(Long id, Long ownerId) throws GenericException;

    ApartmentSearch deactivate(Long id, Long ownerId) throws GenericException;

    ApartmentSearch reactivate(Long id, Long ownerId) throws GenericException;

    void delete(Long id) throws GenericException;
}
