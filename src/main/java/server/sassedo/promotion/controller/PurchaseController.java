package server.sassedo.promotion.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.sassedo.common.data.network.response.PageMeta;
import server.sassedo.common.data.network.response.PagedResponse;
import server.sassedo.model.GenericException;
import server.sassedo.promotion.data.dto.Purchase;
import server.sassedo.promotion.data.network.request.CreatePurchaseRequest;
import server.sassedo.promotion.data.network.response.CheckoutResponse;
import server.sassedo.promotion.data.network.response.PurchaseResponse;
import server.sassedo.promotion.service.PurchaseService;
import server.sassedo.security.jwt.JwtUtils;

import java.util.List;
import java.util.stream.Collectors;

import static server.sassedo.utils.ServerUtils.getUserId;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;
    private final JwtUtils jwtUtils;

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreatePurchaseRequest request, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            PurchaseService.PurchaseResult result = purchaseService.create(userId, request);
            CheckoutResponse response = new CheckoutResponse();
            response.setPurchaseId(result.purchase().getId());
            response.setStatus(result.outcome().status());
            response.setCheckoutUrl(result.outcome().checkoutUrl());
            response.setPayment(PromotionMapper.toPaymentResponse(result.outcome().payment()));
            response.setPromotion(PromotionMapper.toPromotionResponse(result.outcome().promotion()));
            return ResponseEntity.ok(response);
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @GetMapping("/mine")
    public ResponseEntity<?> mine(HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        List<PurchaseResponse> content = purchaseService.getByBuyer(userId).stream()
                .map(PromotionMapper::toPurchaseResponse).collect(Collectors.toList());
        return ResponseEntity.ok(content);
    }

    @GetMapping("/admin/all")
    public ResponseEntity<?> adminAll(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "40") int size) {
        Page<Purchase> purchases = purchaseService.adminAll(PageRequest.of(page, size));
        List<PurchaseResponse> content = purchases.getContent().stream()
                .map(PromotionMapper::toPurchaseResponse).collect(Collectors.toList());
        PageMeta meta = new PageMeta(purchases.getNumber(), purchases.getTotalPages(), purchases.getTotalElements());
        return ResponseEntity.ok(new PagedResponse<>(content, meta));
    }
}
