package server.sassedo.promotion.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.sassedo.listing.rental.data.dto.RentalListing;
import server.sassedo.listing.rental.repository.RentalListingRepository;
import server.sassedo.listing.roommate.data.dto.RoommateListing;
import server.sassedo.listing.roommate.repository.RoommateListingRepository;
import server.sassedo.listing.search.data.dto.ApartmentSearch;
import server.sassedo.listing.search.repository.ApartmentSearchRepository;
import server.sassedo.model.GenericException;
import server.sassedo.model.GenericExceptionCode;
import server.sassedo.promotion.common.ListingType;
import server.sassedo.promotion.common.PromotionType;
import server.sassedo.promotion.data.dto.PromotionState;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PromotableListingServiceImpl implements PromotableListingService {

    private final RentalListingRepository rentalRepository;
    private final RoommateListingRepository roommateRepository;
    private final ApartmentSearchRepository searchRepository;

    @Value("${sassedo.listings.ttl-days:30}")
    private long listingTtlDays;

    @Override
    public Long getOwnerId(ListingType type, Long listingId) throws GenericException {
        return switch (type) {
            case RENTAL -> rentalRepository.findById(listingId).orElseThrow(this::notFound).getOwnerId();
            case ROOMMATE -> roommateRepository.findById(listingId).orElseThrow(this::notFound).getOwnerId();
            case SEARCH -> searchRepository.findById(listingId).orElseThrow(this::notFound).getOwnerId();
        };
    }

    @Override
    @Transactional
    public void applyPromotion(ListingType type, Long listingId, PromotionType promotionType, Long promotionId,
                               LocalDateTime activatedAt, LocalDateTime until, boolean pinned) throws GenericException {
        switch (type) {
            case RENTAL -> {
                RentalListing listing = rentalRepository.findById(listingId).orElseThrow(this::notFound);
                PromotionState state = state(listing.getPromotionState());
                state.applyActive(promotionType, promotionId, activatedAt, until, pinned);
                listing.setPromotionState(state);
                listing.setExpiresAt(activatedAt.plusDays(listingTtlDays));
                rentalRepository.save(listing);
            }
            case ROOMMATE -> {
                RoommateListing listing = roommateRepository.findById(listingId).orElseThrow(this::notFound);
                PromotionState state = state(listing.getPromotionState());
                state.applyActive(promotionType, promotionId, activatedAt, until, pinned);
                listing.setPromotionState(state);
                listing.setExpiresAt(activatedAt.plusDays(listingTtlDays));
                roommateRepository.save(listing);
            }
            case SEARCH -> {
                ApartmentSearch listing = searchRepository.findById(listingId).orElseThrow(this::notFound);
                PromotionState state = state(listing.getPromotionState());
                state.applyActive(promotionType, promotionId, activatedAt, until, pinned);
                listing.setPromotionState(state);
                listing.setExpiresAt(activatedAt.plusDays(listingTtlDays));
                searchRepository.save(listing);
            }
        }
    }

    @Override
    @Transactional
    public void clearPromotion(ListingType type, Long listingId) {
        switch (type) {
            case RENTAL -> rentalRepository.findById(listingId).ifPresent(listing -> {
                PromotionState state = state(listing.getPromotionState());
                state.reset();
                listing.setPromotionState(state);
                rentalRepository.save(listing);
            });
            case ROOMMATE -> roommateRepository.findById(listingId).ifPresent(listing -> {
                PromotionState state = state(listing.getPromotionState());
                state.reset();
                listing.setPromotionState(state);
                roommateRepository.save(listing);
            });
            case SEARCH -> searchRepository.findById(listingId).ifPresent(listing -> {
                PromotionState state = state(listing.getPromotionState());
                state.reset();
                listing.setPromotionState(state);
                searchRepository.save(listing);
            });
        }
    }

    private PromotionState state(PromotionState existing) {
        return existing != null ? existing : new PromotionState();
    }

    private GenericException notFound() {
        return new GenericException(GenericExceptionCode.LISTING_NOT_FOUND, "Listing not found");
    }
}
