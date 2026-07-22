package server.sassedo.promotion.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.sassedo.model.GenericException;
import server.sassedo.model.GenericExceptionCode;
import server.sassedo.promotion.data.dto.PromotionPackage;
import server.sassedo.promotion.data.network.request.PromotionPackageRequest;
import server.sassedo.promotion.repository.PromotionPackageRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PromotionPackageServiceImpl implements PromotionPackageService {

    private final PromotionPackageRepository packageRepository;

    @Override
    public List<PromotionPackage> getActive() {
        return packageRepository.findByActiveTrueOrderBySortPriorityDescPriceCentsAsc();
    }

    @Override
    public List<PromotionPackage> getAll() {
        return packageRepository.findAllByOrderBySortPriorityDescPriceCentsAsc();
    }

    @Override
    public PromotionPackage getById(Long id) throws GenericException {
        return packageRepository.findById(id)
                .orElseThrow(() -> new GenericException(GenericExceptionCode.PROMOTION_PACKAGE_NOT_FOUND,
                        "Promotion package not found"));
    }

    @Override
    @Transactional
    public PromotionPackage create(PromotionPackageRequest request) {
        PromotionPackage pkg = new PromotionPackage();
        apply(pkg, request);
        if (request.getActive() != null) {
            pkg.setActive(request.getActive());
        }
        return packageRepository.save(pkg);
    }

    @Override
    @Transactional
    public PromotionPackage update(Long id, PromotionPackageRequest request) throws GenericException {
        PromotionPackage pkg = getById(id);
        apply(pkg, request);
        if (request.getActive() != null) {
            pkg.setActive(request.getActive());
        }
        return packageRepository.save(pkg);
    }

    @Override
    @Transactional
    public PromotionPackage setActive(Long id, boolean active) throws GenericException {
        PromotionPackage pkg = getById(id);
        pkg.setActive(active);
        return packageRepository.save(pkg);
    }

    private void apply(PromotionPackage pkg, PromotionPackageRequest request) {
        pkg.setNameEn(request.getNameEn());
        pkg.setNameBg(request.getNameBg());
        pkg.setDescription(request.getDescription());
        pkg.setType(request.getType());
        pkg.setDurationDays(request.getDurationDays());
        pkg.setPriceCents(request.getPriceCents());
        pkg.setCurrency(request.getCurrency() != null ? request.getCurrency() : "EUR");
        pkg.setSortPriority(request.getSortPriority());
    }
}
