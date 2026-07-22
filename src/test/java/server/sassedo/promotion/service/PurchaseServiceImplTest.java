package server.sassedo.promotion.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.sassedo.listing.common.ListingStatus;
import server.sassedo.model.GenericException;
import server.sassedo.model.GenericExceptionCode;
import server.sassedo.promotion.common.ListingType;
import server.sassedo.promotion.common.PaymentStatus;
import server.sassedo.promotion.common.PromotionStatus;
import server.sassedo.promotion.common.PromotionType;
import server.sassedo.promotion.data.dto.Promotion;
import server.sassedo.promotion.data.dto.PromotionPackage;
import server.sassedo.promotion.data.dto.Purchase;
import server.sassedo.promotion.data.network.request.CreatePurchaseRequest;
import server.sassedo.promotion.repository.PurchaseRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceImplTest {

    @Mock
    private PurchaseRepository purchaseRepository;
    @Mock
    private PromotionPackageService packageService;
    @Mock
    private PromotionService promotionService;
    @Mock
    private PaymentService paymentService;
    @Mock
    private PromotableListingService listingService;

    @InjectMocks
    private PurchaseServiceImpl service;

    private CreatePurchaseRequest request() {
        CreatePurchaseRequest request = new CreatePurchaseRequest();
        request.setPackageId(5L);
        request.setListingType(ListingType.ROOMMATE);
        request.setListingId(10L);
        return request;
    }

    private PromotionPackage activePackage() {
        PromotionPackage pkg = new PromotionPackage();
        pkg.setId(5L);
        pkg.setType(PromotionType.PROMOTED);
        pkg.setActive(true);
        pkg.setPriceCents(1000);
        pkg.setCurrency("EUR");
        return pkg;
    }

    @Test
    void create_rejectsListingThatIsNotPendingOrActive() throws GenericException {
        when(packageService.getById(5L)).thenReturn(activePackage());
        when(listingService.getOwnerId(ListingType.ROOMMATE, 10L)).thenReturn(1L);
        when(listingService.getListingStatus(ListingType.ROOMMATE, 10L))
                .thenReturn(ListingStatus.REJECTED);

        assertThatThrownBy(() -> service.create(1L, request()))
                .isInstanceOf(GenericException.class)
                .hasFieldOrPropertyWithValue("code", GenericExceptionCode.INVALID_LISTING_STATE);

        verify(promotionService, never()).createPending(anyLong(), any(), any(), anyLong());
    }

    @Test
    void create_allowsPendingListing() throws GenericException {
        when(packageService.getById(5L)).thenReturn(activePackage());
        when(listingService.getOwnerId(ListingType.ROOMMATE, 10L)).thenReturn(1L);
        when(listingService.getListingStatus(ListingType.ROOMMATE, 10L))
                .thenReturn(ListingStatus.PENDING);
        Promotion promotion = new Promotion();
        promotion.setId(2L);
        promotion.setStatus(PromotionStatus.PENDING_PAYMENT);
        when(promotionService.createPending(anyLong(), any(), any(), anyLong()))
                .thenReturn(promotion);
        when(purchaseRepository.save(any())).thenAnswer(inv -> {
            Purchase purchase = inv.getArgument(0);
            purchase.setId(9L);
            return purchase;
        });
        when(promotionService.linkPurchase(promotion, 9L)).thenAnswer(inv -> {
            promotion.setPurchaseId(9L);
            return promotion;
        });
        when(paymentService.startCheckout(any(), any()))
                .thenReturn(new CheckoutOutcome(null, null, PaymentStatus.COMPLETED, promotion));

        PurchaseService.PurchaseResult result = service.create(1L, request());

        assertThat(result).isNotNull();
        assertThat(result.purchase().getId()).isEqualTo(9L);
        assertThat(promotion.getPurchaseId()).isEqualTo(9L);
        verify(promotionService).createPending(anyLong(), any(), any(), anyLong());
        verify(promotionService).linkPurchase(promotion, 9L);
        verify(paymentService).startCheckout(any(Purchase.class), any(Promotion.class));
    }
}
