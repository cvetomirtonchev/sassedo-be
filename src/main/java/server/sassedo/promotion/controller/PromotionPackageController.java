package server.sassedo.promotion.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.sassedo.model.GenericException;
import server.sassedo.promotion.data.network.request.PromotionPackageRequest;
import server.sassedo.promotion.service.PromotionPackageService;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/promotion-packages")
@RequiredArgsConstructor
public class PromotionPackageController {

    private final PromotionPackageService packageService;

    @GetMapping
    public ResponseEntity<?> getActive() {
        List<?> packages = packageService.getActive().stream()
                .map(PromotionMapper::toPackageResponse).collect(Collectors.toList());
        return ResponseEntity.ok(packages);
    }

    @GetMapping("/admin/all")
    public ResponseEntity<?> getAll() {
        List<?> packages = packageService.getAll().stream()
                .map(PromotionMapper::toPackageResponse).collect(Collectors.toList());
        return ResponseEntity.ok(packages);
    }

    @PostMapping("/admin")
    public ResponseEntity<?> create(@Valid @RequestBody PromotionPackageRequest request) {
        return ResponseEntity.ok(PromotionMapper.toPackageResponse(packageService.create(request)));
    }

    @PutMapping("/admin/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody PromotionPackageRequest request) {
        try {
            return ResponseEntity.ok(PromotionMapper.toPackageResponse(packageService.update(id, request)));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<?> deactivate(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(PromotionMapper.toPackageResponse(packageService.setActive(id, false)));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }
}
