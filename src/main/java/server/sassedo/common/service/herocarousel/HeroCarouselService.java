package server.sassedo.common.service.herocarousel;

import org.springframework.web.multipart.MultipartFile;
import server.sassedo.common.data.dto.HeroCarouselSettings;
import server.sassedo.common.data.dto.HeroSlide;
import server.sassedo.common.data.dto.HeroSlideImage;
import server.sassedo.common.data.network.request.AddHeroSlideRequest;
import server.sassedo.common.data.network.request.UpdateHeroSettingsRequest;
import server.sassedo.common.data.network.request.UpdateHeroSlideRequest;
import server.sassedo.model.GenericException;

import java.io.IOException;
import java.util.List;

public interface HeroCarouselService {

    HeroCarouselSettings getSettings();

    HeroCarouselSettings updateSettings(UpdateHeroSettingsRequest request);

    List<HeroSlide> getAllOrdered();

    List<HeroSlide> getEnabledOrdered();

    HeroSlide getById(Long id) throws GenericException;

    HeroSlide add(AddHeroSlideRequest request) throws GenericException;

    HeroSlide update(UpdateHeroSlideRequest request) throws GenericException;

    void delete(Long id) throws GenericException;

    HeroSlideImage setSlideImage(Long slideId, MultipartFile file) throws GenericException, IOException;

    void removeSlideImage(Long slideId) throws GenericException;

    HeroSlideImage getImage(Long id) throws GenericException;
}
