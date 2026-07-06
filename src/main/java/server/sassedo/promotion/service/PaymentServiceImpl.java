package server.sassedo.promotion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.sassedo.model.GenericException;
import server.sassedo.promotion.common.PaymentStatus;
import server.sassedo.promotion.data.dto.Payment;
import server.sassedo.promotion.data.dto.Promotion;
import server.sassedo.promotion.data.dto.Purchase;
import server.sassedo.promotion.payment.CheckoutResult;
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
            resultingPromotion = promotionService.activate(promotion);
        } else if (result.status() == PaymentStatus.FAILED
                || result.status() == PaymentStatus.CANCELLED) {
            promotionService.markPaymentFailed(promotion);
        }

        return new CheckoutOutcome(payment, result.checkoutUrl(), result.status(), resultingPromotion);
    }

    @Override
    @Transactional
    public void handleWebhook(String provider, String rawBody, String signature) {
        WebhookResult result = paymentProvider.handleWebhook(rawBody, signature);
        if (result.providerRef() == null) {
            log.info("Webhook from {} without a provider reference; ignoring", provider);
            return;
        }
        Payment payment = paymentRepository.findFirstByProviderRefOrderByCreatedAtDesc(result.providerRef());
        if (payment == null) {
            log.warn("Webhook referenced unknown payment ref {}", result.providerRef());
            return;
        }
        try {
            confirm(payment, result.status());
        } catch (GenericException e) {
            log.error("Failed to confirm payment {} from webhook: {}", payment.getId(), e.getMessage());
        }
    }

    private void confirm(Payment payment, PaymentStatus status) throws GenericException {
        payment.setStatus(status);
        paymentRepository.save(payment);

        Purchase purchase = purchaseRepository.findById(payment.getPurchaseId()).orElse(null);
        if (purchase == null) {
            return;
        }
        purchase.setStatus(status);
        purchaseRepository.save(purchase);

        if (status == PaymentStatus.COMPLETED && purchase.getPromotionId() != null) {
            Promotion promotion = promotionService.getById(purchase.getPromotionId());
            promotionService.activate(promotion);
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
