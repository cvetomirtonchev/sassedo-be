package server.sassedo.promotion.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import server.sassedo.model.GenericException;
import server.sassedo.promotion.data.dto.Purchase;
import server.sassedo.promotion.data.network.request.CreatePurchaseRequest;

import java.util.List;

public interface PurchaseService {

    PurchaseResult create(Long buyerId, CreatePurchaseRequest request) throws GenericException;

    List<Purchase> getByBuyer(Long buyerId);

    Page<Purchase> adminAll(Pageable pageable);

    /** Holder for the created purchase and its checkout outcome. */
    record PurchaseResult(Purchase purchase, CheckoutOutcome outcome) {
    }
}
