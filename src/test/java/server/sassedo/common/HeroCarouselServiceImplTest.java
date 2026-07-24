package server.sassedo.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import server.sassedo.common.data.dto.FaqLocale;
import server.sassedo.common.data.dto.HeroSlide;
import server.sassedo.common.data.dto.HeroSlideImage;
import server.sassedo.common.data.network.request.AddHeroSlideRequest;
import server.sassedo.common.data.network.request.UpdateHeroSlideRequest;
import server.sassedo.common.repository.HeroCarouselSettingsRepository;
import server.sassedo.common.repository.HeroSlideImageRepository;
import server.sassedo.common.repository.HeroSlideRepository;
import server.sassedo.common.service.herocarousel.HeroCarouselServiceImpl;
import server.sassedo.model.GenericException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeroCarouselServiceImplTest {

    @Mock
    private HeroSlideRepository heroSlideRepository;

    @Mock
    private HeroSlideImageRepository heroSlideImageRepository;

    @Mock
    private HeroCarouselSettingsRepository heroCarouselSettingsRepository;

    @InjectMocks
    private HeroCarouselServiceImpl service;

    private static AddHeroSlideRequest validAddRequest() {
        AddHeroSlideRequest request = new AddHeroSlideRequest();
        request.setTitleBg("  Заглавие  ");
        request.setTitleEn("  Title  ");
        request.setSubtitleBg("  Подзаглавие  ");
        request.setSubtitleEn("  Subtitle  ");
        request.setPrimaryCtaLabelBg("  Търси  ");
        request.setPrimaryCtaLabelEn("  Search  ");
        request.setPrimaryCtaHref("  /find-roommate  ");
        request.setSecondaryCtaLabelBg("Профил");
        request.setSecondaryCtaLabelEn("Profile");
        request.setSecondaryCtaHref("/register");
        request.setSortOrder(2);
        return request;
    }

    @Test
    void getEnabledOrdered_delegatesToEnabledQuery() {
        HeroSlide slide = new HeroSlide();
        when(heroSlideRepository.findAllByEnabledTrueOrderBySortOrderAscIdAsc()).thenReturn(List.of(slide));

        List<HeroSlide> result = service.getEnabledOrdered();

        assertThat(result).containsExactly(slide);
        verify(heroSlideRepository).findAllByEnabledTrueOrderBySortOrderAscIdAsc();
    }

    @Test
    void add_trimsFieldsDefaultsEnabledAndPersists() throws GenericException {
        when(heroSlideRepository.save(any(HeroSlide.class))).thenAnswer(inv -> inv.getArgument(0));

        service.add(validAddRequest());

        ArgumentCaptor<HeroSlide> captor = ArgumentCaptor.forClass(HeroSlide.class);
        verify(heroSlideRepository).save(captor.capture());
        HeroSlide saved = captor.getValue();
        assertThat(saved.getTitleBg()).isEqualTo("Заглавие");
        assertThat(saved.getTitleEn()).isEqualTo("Title");
        assertThat(saved.getPrimaryCtaHref()).isEqualTo("/find-roommate");
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.getSortOrder()).isEqualTo(2);
    }

    @Test
    void add_acceptsSlideWithoutCtas() throws GenericException {
        AddHeroSlideRequest request = validAddRequest();
        request.setPrimaryCtaLabelBg(null);
        request.setPrimaryCtaLabelEn(null);
        request.setPrimaryCtaHref(null);
        request.setSecondaryCtaLabelBg(null);
        request.setSecondaryCtaLabelEn(null);
        request.setSecondaryCtaHref(null);
        when(heroSlideRepository.save(any(HeroSlide.class))).thenAnswer(inv -> inv.getArgument(0));

        HeroSlide saved = service.add(request);

        assertThat(saved.getPrimaryCtaLabelBg()).isNull();
        assertThat(saved.getPrimaryCtaLabelEn()).isNull();
        assertThat(saved.getPrimaryCtaHref()).isNull();
        assertThat(saved.getSecondaryCtaLabelBg()).isNull();
        assertThat(saved.getSecondaryCtaLabelEn()).isNull();
        assertThat(saved.getSecondaryCtaHref()).isNull();
    }

    @Test
    void add_rejectsUnsafeCtaUrl() {
        AddHeroSlideRequest request = validAddRequest();
        request.setPrimaryCtaHref("javascript:alert(1)");

        assertThatThrownBy(() -> service.add(request)).isInstanceOf(GenericException.class);
    }

    @Test
    void update_appliesOnlyProvidedFields() throws GenericException {
        HeroSlide existing = new HeroSlide();
        existing.setTitleBg("bg");
        existing.setTitleEn("en");
        existing.setPrimaryCtaHref("/old");
        existing.setSortOrder(1);
        existing.setEnabled(true);
        when(heroSlideRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(heroSlideRepository.save(any(HeroSlide.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateHeroSlideRequest request = new UpdateHeroSlideRequest();
        request.setId(5L);
        request.setTitleEn("  New EN  ");
        request.setEnabled(false);

        HeroSlide updated = service.update(request);

        assertThat(updated.getTitleEn()).isEqualTo("New EN");
        assertThat(updated.getTitleBg()).isEqualTo("bg");
        assertThat(updated.getPrimaryCtaHref()).isEqualTo("/old");
        assertThat(updated.isEnabled()).isFalse();
    }

    @Test
    void update_clearsPrimaryCtaWithBlankValues() throws GenericException {
        HeroSlide existing = new HeroSlide();
        existing.setPrimaryCtaLabelBg("Търси");
        existing.setPrimaryCtaLabelEn("Search");
        existing.setPrimaryCtaHref("/search");
        when(heroSlideRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(heroSlideRepository.save(any(HeroSlide.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateHeroSlideRequest request = new UpdateHeroSlideRequest();
        request.setId(5L);
        request.setPrimaryCtaLabelBg(" ");
        request.setPrimaryCtaLabelEn("");
        request.setPrimaryCtaHref("  ");

        HeroSlide updated = service.update(request);

        assertThat(updated.getPrimaryCtaLabelBg()).isNull();
        assertThat(updated.getPrimaryCtaLabelEn()).isNull();
        assertThat(updated.getPrimaryCtaHref()).isNull();
    }

    @Test
    void update_throwsWhenMissing() {
        when(heroSlideRepository.findById(404L)).thenReturn(Optional.empty());

        UpdateHeroSlideRequest request = new UpdateHeroSlideRequest();
        request.setId(404L);

        assertThatThrownBy(() -> service.update(request)).isInstanceOf(GenericException.class);
    }

    @Test
    void delete_removesSlideAndItsImage() throws GenericException {
        HeroSlide slide = new HeroSlide();
        slide.setBackgroundImageId(77L);
        when(heroSlideRepository.findById(3L)).thenReturn(Optional.of(slide));

        service.delete(3L);

        verify(heroSlideRepository).delete(slide);
        verify(heroSlideImageRepository).deleteById(77L);
    }

    @Test
    void setSlideImage_storesImageUpdatesSlideAndDeletesPrevious() throws GenericException, IOException {
        HeroSlide slide = new HeroSlide();
        slide.setBackgroundImageId(10L);
        when(heroSlideRepository.findById(1L)).thenReturn(Optional.of(slide));
        when(heroSlideImageRepository.save(any(HeroSlideImage.class))).thenAnswer(inv -> {
            HeroSlideImage image = inv.getArgument(0);
            image.setId(20L);
            return image;
        });

        MockMultipartFile file = new MockMultipartFile(
                "file", "bg.jpg", "image/jpeg", new byte[]{1, 2, 3});

        HeroSlideImage saved = service.setSlideImage(1L, file);

        assertThat(saved.getId()).isEqualTo(20L);
        assertThat(slide.getBackgroundImageId()).isEqualTo(20L);
        verify(heroSlideRepository).save(slide);
        verify(heroSlideImageRepository).deleteById(10L);
    }

    @Test
    void removeSlideImage_clearsReferenceAndDeletesImage() throws GenericException {
        HeroSlide slide = new HeroSlide();
        slide.setBackgroundImageId(42L);
        when(heroSlideRepository.findById(1L)).thenReturn(Optional.of(slide));

        service.removeSlideImage(1L);

        assertThat(slide.getBackgroundImageId()).isNull();
        verify(heroSlideRepository).save(slide);
        verify(heroSlideImageRepository).deleteById(42L);
    }

    @Test
    void entityResolvesTextPerLocaleWithFallback() {
        HeroSlide slide = new HeroSlide();
        slide.setTitleBg("Заглавие");
        slide.setTitleEn("Title");
        assertThat(slide.getTitle(FaqLocale.BG)).isEqualTo("Заглавие");
        assertThat(slide.getTitle(FaqLocale.EN)).isEqualTo("Title");

        HeroSlide bgOnly = new HeroSlide();
        bgOnly.setTitleBg("СамоБГ");
        assertThat(bgOnly.getTitle(FaqLocale.EN)).isEqualTo("СамоБГ");
    }
}
