package server.sassedo.promotion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.sassedo.model.GenericException;
import server.sassedo.promotion.common.PaymentStatus;
import server.sassedo.promotion.common.PromotionStatus;
import server.sassedo.promotion.data.dto.Payment;
import server.sassedo.promotion.data.dto.Promotion;
import server.sassedo.promotion.data.dto.Purchase;
import server.sassedo.promotion.payment.CheckoutResult;
import server.sassedo.promotion.payment.InvalidPaymentWebhookException;
import server.sassedo.promotion.payment.PaymentProvider;
import server.sassedo.promotion.payment.WebhookResult;
import server.sassedo.promotion.repository.PaymentRepository;
import server.sassedo.promotion.repository.PurchaseRepository;

import java.util.List;

@Service
@Log4j2
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentProvider paymentProvider;
    private final PaymentRepository paymentRepository;
    private final PurchaseRepository purchaseRepository;
    private final PromotionService promotionService;

    @Override
    @Transactional
    public CheckoutOutcome startCheckout(Purchase purchase, Promotion promotion) throws GenericException {
        Payment payment = new Payment();
        payment.setPurchaseId(purchase.getId());
        payment.setProvider(paymentProvider.type());
        payment.setAmountCents(purchase.getAmountCents());
        payment.setCurrency(purchase.getCurrency());
        payment.setStatus(PaymentStatus.PENDING);
        payment = paymentRepository.save(payment);

        CheckoutResult result = paymentProvider.createCheckout(purchase);
        payment.setProviderRef(result.providerRef());
        payment.setRawPayload(result.rawPayload());
        payment.setStatus(result.status());
        payment = paymentRepository.save(payment);

        purchase.setStatus(result.status());
        purchaseRepository.save(purchase);

        Promotion resultingPromotion = promotion;
        if (result.status() == PaymentStatus.COMPLETED) {
            resultingPromotion = promotionService.onPaymentCompleted(promotion);
        } else if (result.status() == PaymentStatus.FAILED
                || result.status() == PaymentStatus.CANCELLED) {
            promotionService.markPaymentFailed(promotion);
        }

        return new CheckoutOutcome(payment, result.checkoutUrl(), result.status(), resultingPromotion);
    }

    @Override
    @Transactional
    public void handleWebhook(String provider, String rawBody, String signature) {
        if (provider == null || !paymentProvider.type().name().equalsIgnoreCase(provider)) {
            throw new InvalidPaymentWebhookException("Unsupported payment webhook provider");
        }
        WebhookResult result = paymentProvider.handleWebhook(rawBody, signature);
        if (result.providerRef() == null) {
            log.info("Webhook from {} without a provider reference; ignoring", provider);
            return;
        }
        Payment payment = paymentRepository.findFirstByProviderAndProviderRefOrderByCreatedAtDesc(
                paymentProvider.type(), result.providerRef());
        if (payment == null) {
            throw new IllegalStateException("Webhook referenced unknown payment ref " + result.providerRef());
        }
        try {
            confirm(payment, result.status(), result.rawPayload());
        } catch (GenericException e) {
            throw new IllegalStateException("Failed to confirm payment " + payment.getId(), e);
        }
    }

    private void confirm(Payment payment, PaymentStatus status, String rawPayload) throws GenericException {
        if (payment.getStatus() == status) {
            log.info("Ignoring duplicate {} webhook for payment {}", status, payment.getId());
            return;
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.warn("Ignoring out-of-order payment transition {} -> {} for payment {}",
                    payment.getStatus(), status, payment.getId());
            return;
        }

        payment.setStatus(status);
        payment.setRawPayload(rawPayload);
        paymentRepository.save(payment);

        Purchase purchase = purchaseRepository.findById(payment.getPurchaseId())
                .orElseThrow(() -> new IllegalStateException(
                        "Payment " + payment.getId() + " references a missing purchase"));
        purchase.setStatus(status);
        purchaseRepository.save(purchase);

        if (purchase.getPromotionId() == null) {
            throw new IllegalStateException(
                    "Purchase " + purchase.getId() + " does not reference a promotion");
        }
        Promotion promotion = promotionService.getById(purchase.getPromotionId());
        if (status == PaymentStatus.COMPLETED) {
            // Idempotent: only a still-pending promotion should react to a completed payment,
            // so duplicate or out-of-order webhooks do not re-activate or restart the clock.
            if (promotion.getStatus() == PromotionStatus.PENDING_PAYMENT) {
                promotionService.onPaymentCompleted(promotion);
            }
        } else if (status == PaymentStatus.FAILED || status == PaymentStatus.CANCELLED) {
            if (promotion.getStatus() == PromotionStatus.PENDING_PAYMENT) {
                promotionService.markPaymentFailed(promotion);
            }
        }
    }

    @Override
    public List<Payment> getByBuyer(Long buyerId) {
        List<Purchase> purchases = purchaseRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId);
        List<Long> ids = purchases.stream().map(Purchase::getId).toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return paymentRepository.findByPurchaseIdInOrderByCreatedAtDesc(ids);
    }

    @Override
    public Page<Payment> adminAll(Pageable pageable) {
        return paymentRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
}
