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
import server.sassedo.promotion.common.ListingType;
import server.sassedo.promotion.common.PromotionStatus;
import server.sassedo.promotion.data.dto.Promotion;
import server.sassedo.promotion.data.network.request.GrantPromotionRequest;
import server.sassedo.promotion.data.network.response.PromotionResponse;
import server.sassedo.promotion.service.PromotionService;
import server.sassedo.security.jwt.JwtUtils;

import java.util.List;
import java.util.stream.Collectors;

import static server.sassedo.utils.ServerUtils.getUserId;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;
    private final JwtUtils jwtUtils;

    @GetMapping("/mine")
    public ResponseEntity<?> mine(HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        List<PromotionResponse> content = promotionService.getByOwner(userId).stream()
                .map(PromotionMapper::toPromotionResponse).collect(Collectors.toList());
        return ResponseEntity.ok(content);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            Promotion promotion = promotionService.cancel(id, userId);
            return ResponseEntity.ok(PromotionMapper.toPromotionResponse(promotion));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @GetMapping("/admin/all")
    public ResponseEntity<?> adminAll(
            @RequestParam(required = false) PromotionStatus status,
            @RequestParam(required = false) ListingType listingType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "40") int size) {
        Page<Promotion> promotions = promotionService.adminSearch(status, listingType, PageRequest.of(page, size));
        List<PromotionResponse> content = promotions.getContent().stream()
                .map(PromotionMapper::toPromotionResponse).collect(Collectors.toList());
        PageMeta meta = new PageMeta(promotions.getNumber(), promotions.getTotalPages(), promotions.getTotalElements());
        return ResponseEntity.ok(new PagedResponse<>(content, meta));
    }

    @PostMapping("/admin/grant")
    public ResponseEntity<?> grant(@Valid @RequestBody GrantPromotionRequest request) {
        try {
            Promotion promotion = promotionService.adminGrant(request);
            return ResponseEntity.ok(PromotionMapper.toPromotionResponse(promotion));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @PostMapping("/admin/{id}/remove")
    public ResponseEntity<?> remove(@PathVariable Long id) {
        try {
            Promotion promotion = promotionService.adminRemove(id);
            return ResponseEntity.ok(PromotionMapper.toPromotionResponse(promotion));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }
}
