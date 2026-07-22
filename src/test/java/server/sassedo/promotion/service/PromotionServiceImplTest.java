package server.sassedo.promotion.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.sassedo.listing.common.ListingStatus;
import server.sassedo.model.GenericException;
import server.sassedo.promotion.common.ListingType;
import server.sassedo.promotion.common.PromotionStatus;
import server.sassedo.promotion.common.PromotionType;
import server.sassedo.promotion.data.dto.Promotion;
import server.sassedo.promotion.data.dto.PromotionPackage;
import server.sassedo.promotion.repository.PromotionRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionServiceImplTest {

    @Mock
    private PromotionRepository promotionRepository;
    @Mock
    private PromotionPackageService packageService;
    @Mock
    private PromotableListingService listingService;

    @InjectMocks
    private PromotionServiceImpl service;

    private PromotionPackage pkg() {
        PromotionPackage pkg = new PromotionPackage();
        pkg.setId(5L);
        pkg.setType(PromotionType.PROMOTED);
        pkg.setDurationDays(7);
        return pkg;
    }

    private Promotion promotion(PromotionStatus status) {
        Promotion promotion = new Promotion();
        promotion.setId(1L);
        promotion.setListingType(ListingType.ROOMMATE);
        promotion.setListingId(10L);
        promotion.setPackageId(5L);
        promotion.setType(PromotionType.PROMOTED);
        promotion.setStatus(status);
        return promotion;
    }

    @Test
    void onPaymentCompleted_activeListing_activatesImmediately() throws GenericException {
        Promotion promotion = promotion(PromotionStatus.PENDING_PAYMENT);
        when(listingService.getListingStatus(ListingType.ROOMMATE, 10L))
                .thenReturn(ListingStatus.ACTIVE);
        when(packageService.getById(5L)).thenReturn(pkg());
        when(promotionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Promotion result = service.onPaymentCompleted(promotion);

        assertThat(result.getStatus()).isEqualTo(PromotionStatus.ACTIVE);
        assertThat(result.getStartDate()).isNotNull();
        assertThat(result.getEndDate()).isNotNull();
        verify(listingService).applyPromotion(eq(ListingType.ROOMMATE), eq(10L), eq(PromotionType.PROMOTED),
                any(), any(), any());
    }

    @Test
    void onPaymentCompleted_pendingListing_defersWithoutTouchingListing() throws GenericException {
        Promotion promotion = promotion(PromotionStatus.PENDING_PAYMENT);
        when(listingService.getListingStatus(ListingType.ROOMMATE, 10L))
                .thenReturn(ListingStatus.PENDING);
        when(promotionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Promotion result = service.onPaymentCompleted(promotion);

        assertThat(result.getStatus()).isEqualTo(PromotionStatus.SCHEDULED);
        assertThat(result.getStartDate()).isNull();
        assertThat(result.getEndDate()).isNull();
        verify(packageService, never()).getById(anyLong());
        verify(listingService, never()).applyPromotion(any(), anyLong(), any(), anyLong(),
                any(), any());
    }

    @Test
    void activate_isIdempotentWhenAlreadyActive() throws GenericException {
        Promotion promotion = promotion(PromotionStatus.ACTIVE);

        Promotion result = service.activate(promotion);

        assertThat(result).isSameAs(promotion);
        verify(packageService, never()).getById(anyLong());
        verify(promotionRepository, never()).save(any());
    }

    @Test
    void activateDeferredForListing_activatesParkedPromotion() throws GenericException {
        Promotion parked = promotion(PromotionStatus.SCHEDULED);
        parked.setStartDate(null);
        when(promotionRepository.findByListingTypeAndListingIdAndStatus(
                ListingType.ROOMMATE, 10L, PromotionStatus.SCHEDULED))
                .thenReturn(List.of(parked));
        when(packageService.getById(5L)).thenReturn(pkg());
        when(promotionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.activateDeferredForListing(ListingType.ROOMMATE, 10L);

        assertThat(parked.getStatus()).isEqualTo(PromotionStatus.ACTIVE);
        verify(listingService).applyPromotion(eq(ListingType.ROOMMATE), eq(10L), any(), any(),
                any(), any());
    }

    @Test
    void activateDeferredForListing_skipsFutureDatedPromotion() throws GenericException {
        Promotion future = promotion(PromotionStatus.SCHEDULED);
        future.setStartDate(LocalDateTime.now().plusDays(3));
        when(promotionRepository.findByListingTypeAndListingIdAndStatus(
                ListingType.ROOMMATE, 10L, PromotionStatus.SCHEDULED))
                .thenReturn(List.of(future));

        service.activateDeferredForListing(ListingType.ROOMMATE, 10L);

        assertThat(future.getStatus()).isEqualTo(PromotionStatus.SCHEDULED);
        verify(packageService, never()).getById(anyLong());
    }

    @Test
    void cancelDeferredForListing_cancelsPendingAndScheduled() {
        Promotion pending = promotion(PromotionStatus.PENDING_PAYMENT);
        Promotion scheduled = promotion(PromotionStatus.SCHEDULED);
        scheduled.setId(2L);
        when(promotionRepository.findByListingTypeAndListingIdAndStatusIn(
                eq(ListingType.ROOMMATE), eq(10L), any()))
                .thenReturn(List.of(pending, scheduled));
        when(promotionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.cancelDeferredForListing(ListingType.ROOMMATE, 10L);

        assertThat(pending.getStatus()).isEqualTo(PromotionStatus.CANCELLED);
        assertThat(scheduled.getStatus()).isEqualTo(PromotionStatus.CANCELLED);
        // Only the scheduled one was "live" enough to have written listing state.
        verify(listingService).clearPromotion(ListingType.ROOMMATE, 10L);
    }

    @Test
    void activateApprovedDeferred_activatesWhenListingBecameActive() throws GenericException {
        Promotion parked = promotion(PromotionStatus.SCHEDULED);
        parked.setStartDate(null);
        when(promotionRepository.findByStatusAndStartDateIsNull(PromotionStatus.SCHEDULED))
                .thenReturn(List.of(parked));
        when(listingService.getListingStatus(ListingType.ROOMMATE, 10L))
                .thenReturn(ListingStatus.ACTIVE);
        when(packageService.getById(5L)).thenReturn(pkg());
        when(promotionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int activated = service.activateApprovedDeferred();

        assertThat(activated).isEqualTo(1);
        assertThat(parked.getStatus()).isEqualTo(PromotionStatus.ACTIVE);
    }

    @Test
    void activateApprovedDeferred_leavesPromotionWhenListingStillPending() throws GenericException {
        Promotion parked = promotion(PromotionStatus.SCHEDULED);
        parked.setStartDate(null);
        when(promotionRepository.findByStatusAndStartDateIsNull(PromotionStatus.SCHEDULED))
                .thenReturn(List.of(parked));
        when(listingService.getListingStatus(ListingType.ROOMMATE, 10L))
                .thenReturn(ListingStatus.PENDING);

        int activated = service.activateApprovedDeferred();

        assertThat(activated).isZero();
        assertThat(parked.getStatus()).isEqualTo(PromotionStatus.SCHEDULED);
        verify(packageService, never()).getById(anyLong());
    }
}
