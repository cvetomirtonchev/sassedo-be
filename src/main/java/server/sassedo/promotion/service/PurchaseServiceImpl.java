package server.sassedo.promotion.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.sassedo.model.GenericException;
import server.sassedo.model.GenericExceptionCode;
import server.sassedo.promotion.common.PaymentStatus;
import server.sassedo.promotion.data.dto.Promotion;
import server.sassedo.promotion.data.dto.PromotionPackage;
import server.sassedo.promotion.data.dto.Purchase;
import server.sassedo.promotion.data.network.request.CreatePurchaseRequest;
import server.sassedo.promotion.repository.PurchaseRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PurchaseServiceImpl implements PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final PromotionPackageService packageService;
    private final PromotionService promotionService;
    private final PaymentService paymentService;
    private final PromotableListingService listingService;

    @Override
    @Transactional
    public PurchaseResult create(Long buyerId, CreatePurchaseRequest request) throws GenericException {
        if (buyerId == null) {
            throw new GenericException(GenericExceptionCode.USER_NOT_FOUND, "User not found");
        }
        PromotionPackage pkg = packageService.getById(request.getPackageId());
        if (!pkg.isActive()) {
            throw new GenericException(GenericExceptionCode.PACKAGE_INACTIVE, "Promotion package is not available");
        }

        Long ownerId = listingService.getOwnerId(request.getListingType(), request.getListingId());
        if (!ownerId.equals(buyerId)) {
            throw new GenericException(GenericExceptionCode.NOT_LISTING_OWNER,
                    "You are not the owner of this listing");
        }

        Promotion promotion = promotionService.createPending(buyerId, pkg,
                request.getListingType(), request.getListingId());

        Purchase purchase = new Purchase();
        purchase.setBuyerId(buyerId);
        purchase.setPackageId(pkg.getId());
        purchase.setListingType(request.getListingType());
        purchase.setListingId(request.getListingId());
        purchase.setAmountCents(pkg.getPriceCents());
        purchase.setCurrency(pkg.getCurrency());
        purchase.setStatus(PaymentStatus.PENDING);
        purchase.setPromotionId(promotion.getId());
        purchase = purchaseRepository.save(purchase);

        CheckoutOutcome outcome = paymentService.startCheckout(purchase, promotion);
        return new PurchaseResult(purchase, outcome);
    }

    @Override
    public List<Purchase> getByBuyer(Long buyerId) {
        return purchaseRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId);
    }

    @Override
    public Page<Purchase> adminAll(Pageable pageable) {
        return purchaseRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
}
