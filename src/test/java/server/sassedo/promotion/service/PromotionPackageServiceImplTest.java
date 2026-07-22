package server.sassedo.promotion.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.sassedo.model.GenericException;
import server.sassedo.promotion.common.PromotionType;
import server.sassedo.promotion.controller.PromotionMapper;
import server.sassedo.promotion.data.dto.PromotionPackage;
import server.sassedo.promotion.data.network.request.PromotionPackageRequest;
import server.sassedo.promotion.data.network.response.PromotionPackageResponse;
import server.sassedo.promotion.repository.PromotionPackageRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionPackageServiceImplTest {

    @Mock
    private PromotionPackageRepository packageRepository;

    @InjectMocks
    private PromotionPackageServiceImpl service;

    @Test
    void createMapsBothLocalizedNamesToResponse() {
        when(packageRepository.save(any(PromotionPackage.class))).thenAnswer(invocation -> {
            PromotionPackage pkg = invocation.getArgument(0);
            pkg.setId(7L);
            return pkg;
        });

        PromotionPackage created = service.create(request("Promoted 7 days", "Промотирана за 7 дни"));
        PromotionPackageResponse response = PromotionMapper.toPackageResponse(created);

        assertThat(response.getNameEn()).isEqualTo("Promoted 7 days");
        assertThat(response.getNameBg()).isEqualTo("Промотирана за 7 дни");
    }

    @Test
    void updateReplacesBothLocalizedNamesInResponse() throws GenericException {
        PromotionPackage existing = new PromotionPackage();
        existing.setId(7L);
        existing.setNameEn("Old English");
        existing.setNameBg("Старо име");
        when(packageRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(packageRepository.save(existing)).thenReturn(existing);

        PromotionPackage updated = service.update(
                7L, request("Featured 14 days", "Акцентирана за 14 дни"));
        PromotionPackageResponse response = PromotionMapper.toPackageResponse(updated);

        assertThat(response.getNameEn()).isEqualTo("Featured 14 days");
        assertThat(response.getNameBg()).isEqualTo("Акцентирана за 14 дни");
    }

    private PromotionPackageRequest request(String nameEn, String nameBg) {
        PromotionPackageRequest request = new PromotionPackageRequest();
        request.setNameEn(nameEn);
        request.setNameBg(nameBg);
        request.setType(PromotionType.PROMOTED);
        request.setDurationDays(7);
        request.setPriceCents(499);
        request.setCurrency("EUR");
        request.setSortPriority(10);
        request.setActive(true);
        return request;
    }
}
