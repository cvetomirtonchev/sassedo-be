package server.sassedo.listing.search.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.sassedo.listing.common.ListingFilter;
import server.sassedo.listing.common.ListingStatus;
import server.sassedo.listing.search.data.dto.ApartmentSearch;
import server.sassedo.listing.search.data.network.request.ApartmentSearchRequest;
import server.sassedo.listing.search.repository.ApartmentSearchRepository;
import server.sassedo.listing.search.repository.ApartmentSearchSpecifications;
import server.sassedo.location.data.dto.City;
import server.sassedo.location.data.dto.Country;
import server.sassedo.location.repository.CityRepository;
import server.sassedo.location.repository.CountryRepository;
import server.sassedo.model.GenericException;
import server.sassedo.model.GenericExceptionCode;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApartmentSearchServiceImpl implements ApartmentSearchService {

    private final ApartmentSearchRepository searchRepository;
    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;

    @Value("${sassedo.listings.ttl-days:30}")
    private long listingTtlDays;

    @Override
    @Transactional
    public ApartmentSearch create(Long ownerId, ApartmentSearchRequest request) throws GenericException {
        if (ownerId == null) {
            throw new GenericException(GenericExceptionCode.USER_NOT_FOUND, "User not found");
        }
        ApartmentSearch entity = new ApartmentSearch();
        entity.setOwnerId(ownerId);
        entity.setStatus(ListingStatus.PENDING);
        applyRequest(entity, request);
        return searchRepository.save(entity);
    }

    @Override
    public ApartmentSearch getById(Long id) throws GenericException {
        return searchRepository.findById(id)
                .orElseThrow(() -> new GenericException(GenericExceptionCode.LISTING_NOT_FOUND, "Listing not found"));
    }

    @Override
    public ApartmentSearch getActiveById(Long id) throws GenericException {
        ApartmentSearch entity = getById(id);
        if (entity.getStatus() != ListingStatus.ACTIVE) {
            throw new GenericException(GenericExceptionCode.LISTING_NOT_FOUND, "Listing not found");
        }
        return entity;
    }

    @Override
    public ApartmentSearch getViewableById(Long id, Long requesterId, boolean admin) throws GenericException {
        ApartmentSearch entity = getById(id);
        boolean owner = requesterId != null && requesterId.equals(entity.getOwnerId());
        if (entity.getStatus() != ListingStatus.ACTIVE && !admin && !owner) {
            throw new GenericException(GenericExceptionCode.LISTING_NOT_FOUND, "Listing not found");
        }
        return entity;
    }

    @Override
    public Page<ApartmentSearch> browse(ListingFilter filter, Pageable pageable) {
        return searchRepository.findAll(ApartmentSearchSpecifications.browse(filter), pageable);
    }

    @Override
    public Page<ApartmentSearch> adminSearch(ListingStatus status, String search, Pageable pageable) {
        return searchRepository.adminSearch(status, search, pageable);
    }

    @Override
    public List<ApartmentSearch> getMyListings(Long ownerId) {
        return searchRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId);
    }

    @Override
    @Transactional
    public ApartmentSearch update(Long id, Long ownerId, boolean admin, ApartmentSearchRequest request) throws GenericException {
        ApartmentSearch entity = getById(id);
        if (!admin && (ownerId == null || !ownerId.equals(entity.getOwnerId()))) {
            throw new GenericException(GenericExceptionCode.NOT_LISTING_OWNER, "You are not the owner of this listing");
        }
        applyRequest(entity, request);
        if (!admin) {
            entity.setStatus(ListingStatus.PENDING);
        }
        return searchRepository.save(entity);
    }

    @Override
    @Transactional
    public ApartmentSearch setStatus(Long id, ListingStatus status) throws GenericException {
        if (status == null) {
            throw new GenericException(GenericExceptionCode.INVALID_LISTING_STATUS, "Invalid listing status");
        }
        ApartmentSearch entity = getById(id);
        entity.setStatus(status);
        if (status == ListingStatus.ACTIVE) {
            entity.setExpiresAt(LocalDateTime.now().plusDays(listingTtlDays));
        }
        return searchRepository.save(entity);
    }

    @Override
    @Transactional
    public ApartmentSearch renew(Long id, Long ownerId) throws GenericException {
        ApartmentSearch entity = requireOwner(id, ownerId);
        entity.setExpiresAt(LocalDateTime.now().plusDays(listingTtlDays));
        if (entity.getStatus() == ListingStatus.EXPIRED) {
            entity.setStatus(ListingStatus.ACTIVE);
        }
        return searchRepository.save(entity);
    }

    @Override
    @Transactional
    public ApartmentSearch deactivate(Long id, Long ownerId) throws GenericException {
        ApartmentSearch entity = requireOwner(id, ownerId);
        entity.setStatus(ListingStatus.INACTIVE);
        return searchRepository.save(entity);
    }

    @Override
    @Transactional
    public ApartmentSearch reactivate(Long id, Long ownerId) throws GenericException {
        ApartmentSearch entity = requireOwner(id, ownerId);
        entity.setStatus(ListingStatus.ACTIVE);
        entity.setExpiresAt(LocalDateTime.now().plusDays(listingTtlDays));
        return searchRepository.save(entity);
    }

    private ApartmentSearch requireOwner(Long id, Long ownerId) throws GenericException {
        ApartmentSearch entity = getById(id);
        if (ownerId == null || !ownerId.equals(entity.getOwnerId())) {
            throw new GenericException(GenericExceptionCode.NOT_LISTING_OWNER, "You are not the owner of this listing");
        }
        return entity;
    }

    @Override
    @Transactional
    public void delete(Long id) throws GenericException {
        ApartmentSearch entity = getById(id);
        searchRepository.delete(entity);
    }

    private void applyRequest(ApartmentSearch entity, ApartmentSearchRequest request) throws GenericException {
        entity.setPropertyType(request.getPropertyType());

        if (request.getCountryId() != null) {
            Country country = countryRepository.findById(request.getCountryId())
                    .orElseThrow(() -> new GenericException(GenericExceptionCode.COUNTRY_NOT_FOUND, "Country not found"));
            entity.setCountry(country);
        } else {
            entity.setCountry(null);
        }
        if (request.getCityId() != null) {
            City city = cityRepository.findById(request.getCityId())
                    .orElseThrow(() -> new GenericException(GenericExceptionCode.CITY_NOT_FOUND, "City not found"));
            entity.setCity(city);
        } else {
            entity.setCity(null);
        }
        entity.setNeighborhood(request.getNeighborhood());

        entity.setBudgetMin(request.getBudgetMin());
        entity.setBudgetMax(request.getBudgetMax());
        entity.setAvailableFrom(request.isAvailableAsap() ? null : request.getAvailableFrom());
        entity.setAvailableAsap(request.isAvailableAsap());

        entity.getLeaseTerms().clear();
        if (request.getLeaseTerms() != null) {
            entity.getLeaseTerms().addAll(request.getLeaseTerms());
        }

        entity.setAge(request.getAge());
        entity.setSex(request.getSex());
        entity.setProfession(request.getProfession());
        entity.setSmoker(request.getSmoker());
        entity.setHasPets(request.getHasPets());

        entity.setTitle(request.getTitle());
        entity.setDescription(request.getDescription());
    }
}
