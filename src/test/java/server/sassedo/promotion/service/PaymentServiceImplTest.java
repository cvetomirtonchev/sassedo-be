package server.sassedo.promotion.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.sassedo.model.GenericException;
import server.sassedo.promotion.common.ListingType;
import server.sassedo.promotion.common.PaymentProviderType;
import server.sassedo.promotion.common.PaymentStatus;
import server.sassedo.promotion.common.PromotionStatus;
import server.sassedo.promotion.data.dto.Payment;
import server.sassedo.promotion.data.dto.Promotion;
import server.sassedo.promotion.data.dto.Purchase;
import server.sassedo.promotion.payment.InvalidPaymentWebhookException;
import server.sassedo.promotion.payment.PaymentProvider;
import server.sassedo.promotion.payment.WebhookResult;
import server.sassedo.promotion.repository.PaymentRepository;
import server.sassedo.promotion.repository.PurchaseRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentProvider paymentProvider;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PurchaseRepository purchaseRepository;
    @Mock
    private PromotionService promotionService;

    @InjectMocks
    private PaymentServiceImpl service;

    @BeforeEach
    void setUp() {
        when(paymentProvider.type()).thenReturn(PaymentProviderType.MOCK);
    }

    private Payment payment() {
        Payment payment = new Payment();
        payment.setId(3L);
        payment.setPurchaseId(4L);
        payment.setProvider(PaymentProviderType.MOCK);
        payment.setStatus(PaymentStatus.PENDING);
        return payment;
    }

    private Purchase purchase() {
        Purchase purchase = new Purchase();
        purchase.setId(4L);
        purchase.setPromotionId(7L);
        return purchase;
    }

    @Test
    void handleWebhook_completedPayment_routesThroughOnPaymentCompletedOnce() throws GenericException {
        Promotion promotion = new Promotion();
        promotion.setId(7L);
        promotion.setStatus(PromotionStatus.PENDING_PAYMENT);
        when(paymentProvider.handleWebhook("body", "sig"))
                .thenReturn(new WebhookResult("ref-1", PaymentStatus.COMPLETED, "{}"));
        Payment payment = payment();
        when(paymentRepository.findFirstByProviderAndProviderRefOrderByCreatedAtDesc(
                PaymentProviderType.MOCK, "ref-1"))
                .thenReturn(payment);
        when(purchaseRepository.findById(4L)).thenReturn(Optional.of(purchase()));
        when(promotionService.getById(7L)).thenReturn(promotion);

        service.handleWebhook("mock", "body", "sig");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getRawPayload()).isEqualTo("{}");
        verify(promotionService).onPaymentCompleted(promotion);
    }

    @Test
    void handleWebhook_duplicateCompletedPayment_isIdempotent() throws GenericException {
        Payment payment = payment();
        payment.setStatus(PaymentStatus.COMPLETED);
        when(paymentProvider.handleWebhook("body", "sig"))
                .thenReturn(new WebhookResult("ref-1", PaymentStatus.COMPLETED, "{}"));
        when(paymentRepository.findFirstByProviderAndProviderRefOrderByCreatedAtDesc(
                PaymentProviderType.MOCK, "ref-1"))
                .thenReturn(payment);

        service.handleWebhook("mock", "body", "sig");

        verify(purchaseRepository, never()).findById(any());
        verify(promotionService, never()).onPaymentCompleted(any());
        verify(promotionService, never()).markPaymentFailed(any());
    }

    @Test
    void handleWebhook_failedPayment_marksPromotionFailed() throws GenericException {
        Promotion promotion = new Promotion();
        promotion.setId(7L);
        promotion.setStatus(PromotionStatus.PENDING_PAYMENT);
        when(paymentProvider.handleWebhook("body", "sig"))
                .thenReturn(new WebhookResult("ref-1", PaymentStatus.FAILED, "{}"));
        when(paymentRepository.findFirstByProviderAndProviderRefOrderByCreatedAtDesc(
                PaymentProviderType.MOCK, "ref-1"))
                .thenReturn(payment());
        when(purchaseRepository.findById(4L)).thenReturn(Optional.of(purchase()));
        when(promotionService.getById(7L)).thenReturn(promotion);

        service.handleWebhook("mock", "body", "sig");

        verify(promotionService).markPaymentFailed(promotion);
        verify(promotionService, never()).onPaymentCompleted(any());
    }

    @Test
    void handleWebhook_rejectsWrongProviderRoute() {
        assertThatThrownBy(() -> service.handleWebhook("stripe", "body", "sig"))
                .isInstanceOf(InvalidPaymentWebhookException.class);

        verify(paymentProvider, never()).handleWebhook(any(), any());
    }

    @Test
    void handleWebhook_unknownPaymentFailsSoProviderCanRetry() {
        when(paymentProvider.handleWebhook("body", "sig"))
                .thenReturn(new WebhookResult("ref-unknown", PaymentStatus.COMPLETED, "{}"));
        when(paymentRepository.findFirstByProviderAndProviderRefOrderByCreatedAtDesc(
                PaymentProviderType.MOCK, "ref-unknown"))
                .thenReturn(null);

        assertThatThrownBy(() -> service.handleWebhook("mock", "body", "sig"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unknown payment");
    }

    @Test
    void handleWebhook_missingPurchaseFailsSoTransactionCanRetry() {
        when(paymentProvider.handleWebhook("body", "sig"))
                .thenReturn(new WebhookResult("ref-1", PaymentStatus.COMPLETED, "{}"));
        when(paymentRepository.findFirstByProviderAndProviderRefOrderByCreatedAtDesc(
                PaymentProviderType.MOCK, "ref-1"))
                .thenReturn(payment());
        when(purchaseRepository.findById(4L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handleWebhook("mock", "body", "sig"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing purchase");
    }
}
