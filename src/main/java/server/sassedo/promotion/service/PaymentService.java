package server.sassedo.promotion.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import server.sassedo.model.GenericException;
import server.sassedo.promotion.data.dto.Payment;
import server.sassedo.promotion.data.dto.Promotion;
import server.sassedo.promotion.data.dto.Purchase;

import java.util.List;

public interface PaymentService {

    /** Create a payment attempt, run it through the active provider, and (on COMPLETED)
     * activate the promotion. */
    CheckoutOutcome startCheckout(Purchase purchase, Promotion promotion) throws GenericException;

    /** Provider webhook sink (Stripe-ready). Verifies + confirms a pending payment. */
    void handleWebhook(String provider, String rawBody, String signature);

    List<Payment> getByBuyer(Long buyerId);

    Page<Payment> adminAll(Pageable pageable);
}
