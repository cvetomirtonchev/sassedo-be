package server.sassedo.promotion.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import server.sassedo.promotion.common.PromotionType;
import server.sassedo.promotion.data.dto.PromotionPackage;
import server.sassedo.promotion.repository.PromotionPackageRepository;

/** Seeds the default Promoted / Featured packages (7 / 14 / 30 days) on first startup. */
@Service
@RequiredArgsConstructor
public class PromotionPackageSeeder {

    private final PromotionPackageRepository packageRepository;

    @PostConstruct
    public void init() {
        if (packageRepository.count() > 0) {
            return;
        }
        packageRepository.save(pkg("Promoted 7 days", PromotionType.PROMOTED, 7, 499, 10, false));
        packageRepository.save(pkg("Promoted 14 days", PromotionType.PROMOTED, 14, 899, 11, false));
        packageRepository.save(pkg("Promoted 30 days", PromotionType.PROMOTED, 30, 1499, 12, false));
        packageRepository.save(pkg("Featured 7 days", PromotionType.FEATURED, 7, 1499, 20, true));
        packageRepository.save(pkg("Featured 14 days", PromotionType.FEATURED, 14, 2499, 21, true));
        packageRepository.save(pkg("Featured 30 days", PromotionType.FEATURED, 30, 3999, 22, true));
    }

    private PromotionPackage pkg(String name, PromotionType type, int days, int priceCents,
                                 int sortPriority, boolean pinnable) {
        PromotionPackage p = new PromotionPackage();
        p.setName(name);
        p.setType(type);
        p.setDurationDays(days);
        p.setPriceCents(priceCents);
        p.setCurrency("EUR");
        p.setActive(true);
        p.setSortPriority(sortPriority);
        p.setPinnable(pinnable);
        return p;
    }
}
