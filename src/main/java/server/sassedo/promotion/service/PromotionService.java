package server.sassedo.promotion.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import server.sassedo.model.GenericException;
import server.sassedo.promotion.common.ListingType;
import server.sassedo.promotion.common.PromotionStatus;
import server.sassedo.promotion.data.dto.Promotion;
import server.sassedo.promotion.data.dto.PromotionPackage;
import server.sassedo.promotion.data.network.request.GrantPromotionRequest;

import java.time.LocalDateTime;
import java.util.List;

public interface PromotionService {

    /** Create a PENDING_PAYMENT promotion for a purchase; enforces one-active-per-listing. */
    Promotion createPending(Long ownerId, PromotionPackage pkg, ListingType listingType, Long listingId)
            throws GenericException;

    /** Link the persisted purchase back to its pending promotion for audit and return-status lookup. */
    Promotion linkPurchase(Promotion promotion, Long purchaseId);

    /** Activate a pending promotion after successful payment; writes listing state. Idempotent. */
    Promotion activate(Promotion promotion) throws GenericException;

    /**
     * Route a successfully paid promotion: activate immediately if the listing is already live,
     * otherwise defer it (SCHEDULED with no dates) until the listing is approved.
     */
    Promotion onPaymentCompleted(Promotion promotion) throws GenericException;

    /** Activate any promotion deferred while its listing awaited approval. */
    void activateDeferredForListing(ListingType listingType, Long listingId);

    /** Cancel promotions that were paid/pending while the listing awaited approval. */
    void cancelDeferredForListing(ListingType listingType, Long listingId);

    /** Mark a promotion CANCELLED after a failed/cancelled payment (keeps listing standard). */
    void markPaymentFailed(Promotion promotion);

    /** Admin manual upgrade -> immediately ACTIVE. */
    Promotion adminGrant(GrantPromotionRequest request) throws GenericException;

    /** Admin manual removal -> CANCELLED + downgrade listing. */
    Promotion adminRemove(Long promotionId) throws GenericException;

    /** Owner cancels their own active/scheduled promotion -> CANCELLED + downgrade. */
    Promotion cancel(Long promotionId, Long requesterId) throws GenericException;

    Promotion getById(Long id) throws GenericException;

    List<Promotion> getByOwner(Long ownerId);

    Page<Promotion> adminSearch(PromotionStatus status, ListingType listingType, Pageable pageable);

    /** Scheduler hooks. Return the number of promotions transitioned. */
    int activateScheduled(LocalDateTime now);

    int expireOverdue(LocalDateTime now);

    /**
     * Reconciliation for the rare race where a listing is approved before its paid promotion is
     * recorded as deferred: activates deferred promotions whose listing is now ACTIVE.
     */
    int activateApprovedDeferred();
}
