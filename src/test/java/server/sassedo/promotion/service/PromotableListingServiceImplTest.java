package server.sassedo.promotion.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.sassedo.listing.rental.data.dto.RentalListing;
import server.sassedo.listing.rental.repository.RentalListingRepository;
import server.sassedo.listing.roommate.repository.RoommateListingRepository;
import server.sassedo.promotion.common.ListingType;
import server.sassedo.promotion.common.PromotionType;
import server.sassedo.promotion.data.dto.PromotionState;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotableListingServiceImplTest {

    @Mock
    private RentalListingRepository rentalRepository;
    @Mock
    private RoommateListingRepository roommateRepository;

    @InjectMocks
    private PromotableListingServiceImpl service;

    @Test
    void clearPromotionIfActive_keepsSuccessorStateWhenOldPromotionExpires() {
        RentalListing listing = rentalWithActivePromotion(2L);
        when(rentalRepository.findById(10L)).thenReturn(Optional.of(listing));

        service.clearPromotionIfActive(ListingType.RENTAL, 10L, 1L);

        assertThat(listing.getPromotionState().getActivePromotionId()).isEqualTo(2L);
        assertThat(listing.getPromotionState().getPromotionType()).isEqualTo(PromotionType.FEATURED);
        verify(rentalRepository, never()).save(listing);
    }

    @Test
    void clearPromotionIfActive_resetsStateWhenPromotionStillOwnsListingTier() {
        RentalListing listing = rentalWithActivePromotion(1L);
        when(rentalRepository.findById(10L)).thenReturn(Optional.of(listing));

        service.clearPromotionIfActive(ListingType.RENTAL, 10L, 1L);

        assertThat(listing.getPromotionState().getActivePromotionId()).isNull();
        assertThat(listing.getPromotionState().getPromotionType()).isNull();
        assertThat(listing.getPromotionState().getPromotionPriority()).isZero();
        verify(rentalRepository).save(listing);
    }

    private RentalListing rentalWithActivePromotion(Long promotionId) {
        RentalListing listing = new RentalListing();
        PromotionState state = new PromotionState();
        LocalDateTime now = LocalDateTime.now();
        state.applyActive(PromotionType.FEATURED, promotionId, now, now.plusDays(7));
        listing.setPromotionState(state);
        return listing;
    }
}
