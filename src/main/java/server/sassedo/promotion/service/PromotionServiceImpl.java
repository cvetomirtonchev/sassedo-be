package server.sassedo.promotion.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.sassedo.model.GenericException;
import server.sassedo.model.GenericExceptionCode;
import server.sassedo.promotion.common.ListingType;
import server.sassedo.promotion.common.PromotionSource;
import server.sassedo.promotion.common.PromotionStatus;
import server.sassedo.promotion.common.PromotionType;
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
        promotion.setPinned(pkg.getType() == PromotionType.FEATURED && pkg.isPinnable());
        return promotionRepository.save(promotion);
    }

    @Override
    @Transactional
    public Promotion activate(Promotion promotion) throws GenericException {
        PromotionPackage pkg = packageService.getById(promotion.getPackageId());
        LocalDateTime now = LocalDateTime.now();
        promotion.setStartDate(now);
        promotion.setEndDate(now.plusDays(pkg.getDurationDays()));
        promotion.setStatus(PromotionStatus.ACTIVE);
        Promotion saved = promotionRepository.save(promotion);
        listingService.applyPromotion(saved.getListingType(), saved.getListingId(), saved.getType(),
                saved.getId(), saved.getStartDate(), saved.getEndDate(), saved.isPinned());
        return saved;
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
        promotion.setPinned(request.getType() == PromotionType.FEATURED && request.isPinned());
        promotion.setStartDate(now);
        promotion.setEndDate(now.plusDays(request.getDurationDays()));
        Promotion saved = promotionRepository.save(promotion);
        listingService.applyPromotion(saved.getListingType(), saved.getListingId(), saved.getType(),
                saved.getId(), saved.getStartDate(), saved.getEndDate(), saved.isPinned());
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
        Promotion promotion = getById(promotionId);
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
                        promotion.getEndDate(), promotion.isPinned());
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
            listingService.clearPromotion(promotion.getListingType(), promotion.getListingId());
        }
        return overdue.size();
    }

    private void ensureListingNotPromoted(ListingType listingType, Long listingId) throws GenericException {
        List<Promotion> existing = promotionRepository.findByListingTypeAndListingIdAndStatusIn(
                listingType, listingId, BLOCKING_STATUSES);
        if (!existing.isEmpty()) {
            throw new GenericException(GenericExceptionCode.LISTING_ALREADY_PROMOTED,
                    "This listing already has an active or pending promotion");
        }
    }

    /** Transition to a terminal status and, if it was live, downgrade the listing. */
    private void terminate(Promotion promotion, PromotionStatus terminalStatus) {
        boolean wasLive = promotion.getStatus() == PromotionStatus.ACTIVE
                || promotion.getStatus() == PromotionStatus.SCHEDULED;
        promotion.setStatus(terminalStatus);
        promotionRepository.save(promotion);
        if (wasLive) {
            listingService.clearPromotion(promotion.getListingType(), promotion.getListingId());
        }
    }
}
