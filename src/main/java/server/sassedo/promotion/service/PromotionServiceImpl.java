package server.sassedo.promotion.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.sassedo.listing.common.ListingStatus;
import server.sassedo.model.GenericException;
import server.sassedo.model.GenericExceptionCode;
import server.sassedo.promotion.common.ListingType;
import server.sassedo.promotion.common.PromotionSource;
import server.sassedo.promotion.common.PromotionStatus;
import server.sassedo.promotion.data.dto.Promotion;
import server.sassedo.promotion.data.dto.PromotionPackage;
import server.sassedo.promotion.data.network.request.GrantPromotionRequest;
import server.sassedo.promotion.repository.PromotionRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private static final List<PromotionStatus> BLOCKING_STATUSES = List.of(
            PromotionStatus.PENDING_PAYMENT, PromotionStatus.SCHEDULED, PromotionStatus.ACTIVE);

    private static final List<PromotionStatus> QUEUED_RENEWAL_STATUSES = List.of(
            PromotionStatus.PENDING_PAYMENT, PromotionStatus.SCHEDULED);

    private final PromotionRepository promotionRepository;
    private final PromotionPackageService packageService;
    private final PromotableListingService listingService;

    @Override
    @Transactional
    public Promotion createPending(Long ownerId, PromotionPackage pkg, ListingType listingType, Long listingId)
            throws GenericException {
        ensureListingNotPromoted(listingType, listingId);
        Promotion promotion = new Promotion();
        promotion.setListingType(listingType);
        promotion.setListingId(listingId);
        promotion.setOwnerId(ownerId);
        promotion.setPackageId(pkg.getId());
        promotion.setType(pkg.getType());
        promotion.setStatus(PromotionStatus.PENDING_PAYMENT);
        promotion.setSource(PromotionSource.PURCHASE);
        return promotionRepository.save(promotion);
    }

    @Override
    @Transactional
    public Promotion createPendingRenewal(Long ownerId, PromotionPackage pkg, ListingType listingType,
                                          Long listingId, Long renewFromPromotionId) throws GenericException {
        Promotion predecessor = promotionRepository.findByIdForUpdate(renewFromPromotionId)
                .orElseThrow(() -> new GenericException(GenericExceptionCode.PROMOTION_NOT_FOUND,
                        "Promotion to renew from was not found"));

        if (!ownerId.equals(predecessor.getOwnerId())) {
            throw new GenericException(GenericExceptionCode.NOT_PROMOTION_OWNER,
                    "You are not the owner of this promotion");
        }
        if (predecessor.getListingType() != listingType || !predecessor.getListingId().equals(listingId)) {
            throw new GenericException(GenericExceptionCode.RENEWAL_NOT_ALLOWED,
                    "Renewal must target the same listing as the active promotion");
        }
        if (predecessor.getStatus() != PromotionStatus.ACTIVE) {
            throw new GenericException(GenericExceptionCode.RENEWAL_NOT_ALLOWED,
                    "Only an active promotion can be renewed");
        }
        LocalDateTime now = LocalDateTime.now();
        if (predecessor.getEndDate() == null || !predecessor.getEndDate().isAfter(now)) {
            throw new GenericException(GenericExceptionCode.RENEWAL_NOT_ALLOWED,
                    "Renewal is only allowed while the current promotion has time remaining");
        }
        ListingStatus listingStatus = listingService.getListingStatus(listingType, listingId);
        if (listingStatus != ListingStatus.ACTIVE) {
            throw new GenericException(GenericExceptionCode.RENEWAL_NOT_ALLOWED,
                    "Renewals require an active listing with a live promotion");
        }

        ensureRenewalSlotAvailable(listingType, listingId, renewFromPromotionId);

        if (promotionRepository.existsByPredecessorPromotionIdAndStatusIn(
                renewFromPromotionId, QUEUED_RENEWAL_STATUSES)) {
            throw new GenericException(GenericExceptionCode.RENEWAL_ALREADY_QUEUED,
                    "A renewal is already queued for this promotion");
        }

        Promotion promotion = new Promotion();
        promotion.setListingType(listingType);
        promotion.setListingId(listingId);
        promotion.setOwnerId(ownerId);
        promotion.setPackageId(pkg.getId());
        promotion.setType(pkg.getType());
        promotion.setStatus(PromotionStatus.PENDING_PAYMENT);
        promotion.setSource(PromotionSource.PURCHASE);
        promotion.setPredecessorPromotionId(renewFromPromotionId);
        return promotionRepository.save(promotion);
    }

    @Override
    @Transactional
    public Promotion linkPurchase(Promotion promotion, Long purchaseId) {
        promotion.setPurchaseId(purchaseId);
        return promotionRepository.save(promotion);
    }

    @Override
    @Transactional
    public Promotion activate(Promotion promotion) throws GenericException {
        // Idempotent: an already-active promotion must not restart its clock or double-apply.
        if (promotion.getStatus() == PromotionStatus.ACTIVE) {
            return promotion;
        }
        PromotionPackage pkg = packageService.getById(promotion.getPackageId());
        LocalDateTime now = LocalDateTime.now();
        promotion.setStartDate(now);
        promotion.setEndDate(now.plusDays(pkg.getDurationDays()));
        promotion.setStatus(PromotionStatus.ACTIVE);
        Promotion saved = promotionRepository.save(promotion);
        listingService.applyPromotion(saved.getListingType(), saved.getListingId(), saved.getType(),
                saved.getId(), saved.getStartDate(), saved.getEndDate());
        return saved;
    }

    @Override
    @Transactional
    public Promotion onPaymentCompleted(Promotion promotion) throws GenericException {
        Promotion current = promotionRepository.findByIdForUpdate(promotion.getId())
                .orElseThrow(() -> new GenericException(GenericExceptionCode.PROMOTION_NOT_FOUND,
                        "Promotion not found"));
        if (current.getStatus() != PromotionStatus.PENDING_PAYMENT) {
            return current;
        }
        if (current.getPredecessorPromotionId() != null) {
            return onRenewalPaymentCompleted(current);
        }
        ListingStatus listingStatus = listingService.getListingStatus(
                current.getListingType(), current.getListingId());
        if (listingStatus == ListingStatus.ACTIVE) {
            return activate(current);
        }
        // The listing is still awaiting approval: park the promotion as SCHEDULED with no dates
        // so the time-based scheduler ignores it. It starts when the listing is approved.
        current.setStartDate(null);
        current.setEndDate(null);
        current.setStatus(PromotionStatus.SCHEDULED);
        return promotionRepository.save(current);
    }

    private Promotion onRenewalPaymentCompleted(Promotion promotion) throws GenericException {
        Promotion predecessor = promotionRepository.findByIdForUpdate(promotion.getPredecessorPromotionId())
                .orElse(null);
        if (predecessor != null && predecessor.getStatus() == PromotionStatus.CANCELLED) {
            promotion.setStatus(PromotionStatus.CANCELLED);
            return promotionRepository.save(promotion);
        }

        LocalDateTime now = LocalDateTime.now();
        boolean predecessorStillActive = predecessor != null
                && predecessor.getStatus() == PromotionStatus.ACTIVE
                && predecessor.getEndDate() != null
                && predecessor.getEndDate().isAfter(now);

        if (predecessorStillActive) {
            PromotionPackage pkg = packageService.getById(promotion.getPackageId());
            LocalDateTime start = predecessor.getEndDate();
            promotion.setStartDate(start);
            promotion.setEndDate(start.plusDays(pkg.getDurationDays()));
            promotion.setStatus(PromotionStatus.SCHEDULED);
            return promotionRepository.save(promotion);
        }

        ListingStatus listingStatus = listingService.getListingStatus(
                promotion.getListingType(), promotion.getListingId());
        if (listingStatus == ListingStatus.ACTIVE) {
            return activate(promotion);
        }
        promotion.setStartDate(null);
        promotion.setEndDate(null);
        promotion.setStatus(PromotionStatus.SCHEDULED);
        return promotionRepository.save(promotion);
    }

    @Override
    @Transactional
    public void activateDeferredForListing(ListingType listingType, Long listingId) {
        List<Promotion> deferred = promotionRepository.findByListingTypeAndListingIdAndStatus(
                listingType, listingId, PromotionStatus.SCHEDULED);
        for (Promotion promotion : deferred) {
            // Only promotions parked for approval (no start date) should begin now; genuine
            // future-dated promotions keep their own schedule.
            if (promotion.getStartDate() == null) {
                try {
                    activate(promotion);
                } catch (GenericException ignored) {
                    // Missing package; leave the promotion for admin follow-up.
                }
            }
        }
    }

    @Override
    @Transactional
    public void cancelDeferredForListing(ListingType listingType, Long listingId) {
        List<Promotion> deferred = promotionRepository.findByListingTypeAndListingIdAndStatusIn(
                listingType, listingId,
                List.of(PromotionStatus.PENDING_PAYMENT, PromotionStatus.SCHEDULED));
        for (Promotion promotion : deferred) {
            terminate(promotion, PromotionStatus.CANCELLED);
        }
    }

    @Override
    @Transactional
    public void markPaymentFailed(Promotion promotion) {
        promotion.setStatus(PromotionStatus.CANCELLED);
        promotionRepository.save(promotion);
    }

    @Override
    @Transactional
    public Promotion adminGrant(GrantPromotionRequest request) throws GenericException {
        Long ownerId = listingService.getOwnerId(request.getListingType(), request.getListingId());
        ensureListingNotPromoted(request.getListingType(), request.getListingId());
        LocalDateTime now = LocalDateTime.now();
        Promotion promotion = new Promotion();
        promotion.setListingType(request.getListingType());
        promotion.setListingId(request.getListingId());
        promotion.setOwnerId(ownerId);
        promotion.setType(request.getType());
        promotion.setStatus(PromotionStatus.ACTIVE);
        promotion.setSource(PromotionSource.ADMIN_GRANT);
        promotion.setStartDate(now);
        promotion.setEndDate(now.plusDays(request.getDurationDays()));
        Promotion saved = promotionRepository.save(promotion);
        listingService.applyPromotion(saved.getListingType(), saved.getListingId(), saved.getType(),
                saved.getId(), saved.getStartDate(), saved.getEndDate());
        return saved;
    }

    @Override
    @Transactional
    public Promotion adminRemove(Long promotionId) throws GenericException {
        Promotion promotion = getById(promotionId);
        terminate(promotion, PromotionStatus.CANCELLED);
        return promotion;
    }

    @Override
    @Transactional
    public Promotion cancel(Long promotionId, Long requesterId) throws GenericException {
        Promotion promotion = promotionRepository.findByIdForUpdate(promotionId)
                .orElseThrow(() -> new GenericException(GenericExceptionCode.PROMOTION_NOT_FOUND,
                        "Promotion not found"));
        if (requesterId == null || !requesterId.equals(promotion.getOwnerId())) {
            throw new GenericException(GenericExceptionCode.NOT_PROMOTION_OWNER,
                    "You are not the owner of this promotion");
        }
        if (!BLOCKING_STATUSES.contains(promotion.getStatus())) {
            throw new GenericException(GenericExceptionCode.INVALID_PROMOTION_REQUEST,
                    "Promotion cannot be cancelled");
        }
        terminate(promotion, PromotionStatus.CANCELLED);
        return promotion;
    }

    @Override
    @Transactional
    public int cancelAllForOwner(Long ownerId) {
        List<Promotion> blocking = promotionRepository.findByOwnerIdAndStatusInForUpdate(
                ownerId, BLOCKING_STATUSES);
        for (Promotion promotion : blocking) {
            // Cancelling an active predecessor also cancels its queued successors. Skip those
            // successor rows when the locked result set reaches them.
            if (!BLOCKING_STATUSES.contains(promotion.getStatus())) {
                continue;
            }
            terminate(promotion, PromotionStatus.CANCELLED);
        }
        return blocking.size();
    }

    @Override
    public Promotion getById(Long id) throws GenericException {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new GenericException(GenericExceptionCode.PROMOTION_NOT_FOUND,
                        "Promotion not found"));
    }

    @Override
    public List<Promotion> getByOwner(Long ownerId) {
        return promotionRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId);
    }

    @Override
    public Page<Promotion> adminSearch(PromotionStatus status, ListingType listingType, Pageable pageable) {
        return promotionRepository.adminSearch(status, listingType, pageable);
    }

    @Override
    @Transactional
    public int activateScheduled(LocalDateTime now) {
        List<Promotion> due = promotionRepository.findByStatusAndStartDateLessThanEqual(
                PromotionStatus.SCHEDULED, now);
        for (Promotion promotion : due) {
            promotion.setStatus(PromotionStatus.ACTIVE);
            promotionRepository.save(promotion);
            try {
                listingService.applyPromotion(promotion.getListingType(), promotion.getListingId(),
                        promotion.getType(), promotion.getId(), promotion.getStartDate(),
                        promotion.getEndDate());
            } catch (GenericException ignored) {
                // Listing was removed; the promotion status is still advanced for audit.
            }
        }
        return due.size();
    }

    @Override
    @Transactional
    public int expireOverdue(LocalDateTime now) {
        List<Promotion> overdue = promotionRepository.findByStatusAndEndDateLessThanEqual(
                PromotionStatus.ACTIVE, now);
        for (Promotion promotion : overdue) {
            promotion.setStatus(PromotionStatus.EXPIRED);
            promotionRepository.save(promotion);
            listingService.clearPromotionIfActive(promotion.getListingType(), promotion.getListingId(),
                    promotion.getId());
        }
        return overdue.size();
    }

    @Override
    @Transactional
    public int activateApprovedDeferred() {
        List<Promotion> deferred = promotionRepository.findByStatusAndStartDateIsNull(
                PromotionStatus.SCHEDULED);
        int activated = 0;
        for (Promotion promotion : deferred) {
            try {
                ListingStatus listingStatus = listingService.getListingStatus(
                        promotion.getListingType(), promotion.getListingId());
                if (listingStatus == ListingStatus.ACTIVE) {
                    activate(promotion);
                    activated++;
                }
            } catch (GenericException ignored) {
                // Listing removed or package missing; leave the promotion for audit.
            }
        }
        return activated;
    }

    private void ensureListingNotPromoted(ListingType listingType, Long listingId) throws GenericException {
        List<Promotion> existing = promotionRepository.findByListingTypeAndListingIdAndStatusIn(
                listingType, listingId, BLOCKING_STATUSES);
        if (!existing.isEmpty()) {
            throw new GenericException(GenericExceptionCode.LISTING_ALREADY_PROMOTED,
                    "This listing already has an active or pending promotion");
        }
    }

    private void ensureRenewalSlotAvailable(ListingType listingType, Long listingId, Long predecessorId)
            throws GenericException {
        List<Promotion> blocking = promotionRepository.findByListingTypeAndListingIdAndStatusIn(
                listingType, listingId, BLOCKING_STATUSES);
        for (Promotion existing : blocking) {
            if (existing.getId().equals(predecessorId)) {
                continue;
            }
            if (existing.getPredecessorPromotionId() != null
                    && existing.getPredecessorPromotionId().equals(predecessorId)
                    && QUEUED_RENEWAL_STATUSES.contains(existing.getStatus())) {
                throw new GenericException(GenericExceptionCode.RENEWAL_ALREADY_QUEUED,
                        "A renewal is already queued for this promotion");
            }
            throw new GenericException(GenericExceptionCode.LISTING_ALREADY_PROMOTED,
                    "This listing already has an active or pending promotion");
        }
    }

    /** Transition to a terminal status; downgrade listing only when this row was the live tier. */
    private void terminate(Promotion promotion, PromotionStatus terminalStatus) {
        PromotionStatus previous = promotion.getStatus();
        promotion.setStatus(terminalStatus);
        promotionRepository.save(promotion);
        if (previous == PromotionStatus.ACTIVE) {
            listingService.clearPromotionIfActive(promotion.getListingType(), promotion.getListingId(),
                    promotion.getId());
            cancelQueuedSuccessors(promotion.getId());
        }
    }

    private void cancelQueuedSuccessors(Long predecessorPromotionId) {
        List<Promotion> successors = promotionRepository.findByPredecessorPromotionIdAndStatusIn(
                predecessorPromotionId, QUEUED_RENEWAL_STATUSES);
        for (Promotion successor : successors) {
            successor.setStatus(PromotionStatus.CANCELLED);
            promotionRepository.save(successor);
        }
    }
}
