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
        packageRepository.save(pkg(
                "Promoted 7 days", "Промотирана за 7 дни", PromotionType.PROMOTED, 7, 499, 10));
        packageRepository.save(pkg(
                "Promoted 14 days", "Промотирана за 14 дни", PromotionType.PROMOTED, 14, 899, 11));
        packageRepository.save(pkg(
                "Promoted 30 days", "Промотирана за 30 дни", PromotionType.PROMOTED, 30, 1499, 12));
        packageRepository.save(pkg(
                "Featured 7 days", "Акцентирана за 7 дни", PromotionType.FEATURED, 7, 1499, 20));
        packageRepository.save(pkg(
                "Featured 14 days", "Акцентирана за 14 дни", PromotionType.FEATURED, 14, 2499, 21));
        packageRepository.save(pkg(
                "Featured 30 days", "Акцентирана за 30 дни", PromotionType.FEATURED, 30, 3999, 22));
    }

    private PromotionPackage pkg(String nameEn, String nameBg, PromotionType type, int days, int priceCents,
                                 int sortPriority) {
        PromotionPackage p = new PromotionPackage();
        p.setNameEn(nameEn);
        p.setNameBg(nameBg);
        p.setType(type);
        p.setDurationDays(days);
        p.setPriceCents(priceCents);
        p.setCurrency("EUR");
        p.setActive(true);
        p.setSortPriority(sortPriority);
        return p;
    }
}
