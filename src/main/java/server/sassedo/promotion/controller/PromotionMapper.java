package server.sassedo.promotion.controller;

import server.sassedo.promotion.common.PromotionStatus;
import server.sassedo.promotion.data.dto.Payment;
import server.sassedo.promotion.data.dto.Promotion;
import server.sassedo.promotion.data.dto.PromotionPackage;
import server.sassedo.promotion.data.dto.Purchase;
import server.sassedo.promotion.data.network.response.PaymentResponse;
import server.sassedo.promotion.data.network.response.PromotionPackageResponse;
import server.sassedo.promotion.data.network.response.PromotionResponse;
import server.sassedo.promotion.data.network.response.PurchaseResponse;

import java.time.Duration;
import java.time.LocalDateTime;

/** Manual entity -> response mapping (repo convention: no MapStruct). */
public final class PromotionMapper {

    private PromotionMapper() {
    }

    public static PromotionPackageResponse toPackageResponse(PromotionPackage p) {
        PromotionPackageResponse r = new PromotionPackageResponse();
        r.setId(p.getId());
        r.setName(p.getName());
        r.setDescription(p.getDescription());
        r.setType(p.getType());
        r.setDurationDays(p.getDurationDays());
        r.setPriceCents(p.getPriceCents());
        r.setCurrency(p.getCurrency());
        r.setActive(p.isActive());
        r.setSortPriority(p.getSortPriority());
        r.setPinnable(p.isPinnable());
        return r;
    }

    public static PromotionResponse toPromotionResponse(Promotion p) {
        PromotionResponse r = new PromotionResponse();
        r.setId(p.getId());
        r.setListingType(p.getListingType());
        r.setListingId(p.getListingId());
        r.setOwnerId(p.getOwnerId());
        r.setPackageId(p.getPackageId());
        r.setType(p.getType());
        r.setStatus(p.getStatus());
        r.setSource(p.getSource());
        r.setStartDate(p.getStartDate());
        r.setEndDate(p.getEndDate());
        r.setPurchaseId(p.getPurchaseId());
        r.setPinned(p.isPinned());
        r.setCreatedAt(p.getCreatedAt());
        if (p.getStatus() == PromotionStatus.ACTIVE && p.getEndDate() != null) {
            long days = Duration.between(LocalDateTime.now(), p.getEndDate()).toDays();
            r.setRemainingDays(Math.max(0, days));
        }
        return r;
    }

    public static PurchaseResponse toPurchaseResponse(Purchase p) {
        PurchaseResponse r = new PurchaseResponse();
        r.setId(p.getId());
        r.setBuyerId(p.getBuyerId());
        r.setPackageId(p.getPackageId());
        r.setListingType(p.getListingType());
        r.setListingId(p.getListingId());
        r.setAmountCents(p.getAmountCents());
        r.setCurrency(p.getCurrency());
        r.setStatus(p.getStatus());
        r.setPromotionId(p.getPromotionId());
        r.setCreatedAt(p.getCreatedAt());
        return r;
    }

    public static PaymentResponse toPaymentResponse(Payment p) {
        PaymentResponse r = new PaymentResponse();
        r.setId(p.getId());
        r.setPurchaseId(p.getPurchaseId());
        r.setProvider(p.getProvider());
        r.setProviderRef(p.getProviderRef());
        r.setStatus(p.getStatus());
        r.setAmountCents(p.getAmountCents());
        r.setCurrency(p.getCurrency());
        r.setCreatedAt(p.getCreatedAt());
        return r;
    }
}
