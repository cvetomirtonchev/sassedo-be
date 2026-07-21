package server.sassedo.common.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import server.sassedo.common.data.dto.FaqLocale;
import server.sassedo.common.data.dto.HeroCarouselSettings;
import server.sassedo.common.data.dto.HeroSlide;
import server.sassedo.common.data.dto.HeroSlideImage;
import server.sassedo.common.data.network.request.AddHeroSlideRequest;
import server.sassedo.common.data.network.request.UpdateHeroSettingsRequest;
import server.sassedo.common.data.network.request.UpdateHeroSlideRequest;
import server.sassedo.common.data.network.response.HeroCarouselPublicResponse;
import server.sassedo.common.data.network.response.HeroCarouselSettingsResponse;
import server.sassedo.common.data.network.response.HeroSlideAdminResponse;
import server.sassedo.common.data.network.response.HeroSlideImageUploadResponse;
import server.sassedo.common.data.network.response.HeroSlidePublicResponse;
import server.sassedo.common.service.herocarousel.HeroCarouselService;
import server.sassedo.model.GenericException;
import server.sassedo.utils.ImageResponses;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/hero-carousel")
@RequiredArgsConstructor
public class HeroCarouselController {

    private final HeroCarouselService heroCarouselService;

    @GetMapping
    public ResponseEntity<?> getCarousel(@RequestParam(defaultValue = "bg") String locale) {
        try {
            FaqLocale heroLocale = FaqLocale.fromCode(locale);
            HeroCarouselSettings settings = heroCarouselService.getSettings();
            List<HeroSlidePublicResponse> slides = heroCarouselService.getEnabledOrdered().stream()
                    .map(slide -> toPublicResponse(slide, heroLocale))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(new HeroCarouselPublicResponse(settings.getBadge(heroLocale), slides));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @GetMapping("/images/{imageId}")
    public ResponseEntity<byte[]> getImage(@PathVariable Long imageId,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        try {
            return imageResponse(heroCarouselService.getImage(imageId), ifNoneMatch);
        } catch (GenericException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/admin/settings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getSettings() {
        HeroCarouselSettings settings = heroCarouselService.getSettings();
        return ResponseEntity.ok(new HeroCarouselSettingsResponse(settings.getBadgeBg(), settings.getBadgeEn()));
    }

    @PutMapping("/admin/settings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateSettings(@Valid @RequestBody UpdateHeroSettingsRequest request) {
        HeroCarouselSettings settings = heroCarouselService.updateSettings(request);
        return ResponseEntity.ok(new HeroCarouselSettingsResponse(settings.getBadgeBg(), settings.getBadgeEn()));
    }

    @GetMapping("/admin/slides")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllForAdmin() {
        List<HeroSlideAdminResponse> slides = heroCarouselService.getAllOrdered().stream()
                .map(this::toAdminResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(slides);
    }

    @GetMapping("/admin/slides/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getSlideById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(toAdminResponse(heroCarouselService.getById(id)));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @PostMapping("/admin/slides")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addSlide(@Valid @RequestBody AddHeroSlideRequest request) {
        try {
            return ResponseEntity.ok(toAdminResponse(heroCarouselService.add(request)));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @PutMapping("/admin/slides")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateSlide(@Valid @RequestBody UpdateHeroSlideRequest request) {
        try {
            return ResponseEntity.ok(toAdminResponse(heroCarouselService.update(request)));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @DeleteMapping("/admin/slides/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteSlide(@PathVariable Long id) {
        try {
            heroCarouselService.delete(id);
            return ResponseEntity.ok().build();
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @PostMapping(value = "/admin/slides/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadSlideImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            HeroSlideImage image = heroCarouselService.setSlideImage(id, file);
            return ResponseEntity.ok(new HeroSlideImageUploadResponse(image.getId(), buildImageUrl(image.getId())));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Could not read uploaded file");
        }
    }

    @DeleteMapping("/admin/slides/{id}/image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> removeSlideImage(@PathVariable Long id) {
        try {
            heroCarouselService.removeSlideImage(id);
            return ResponseEntity.ok().build();
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    private HeroSlidePublicResponse toPublicResponse(HeroSlide slide, FaqLocale locale) {
        HeroSlidePublicResponse r = new HeroSlidePublicResponse();
        r.setId(slide.getId());
        r.setTitle(slide.getTitle(locale));
        r.setSubtitle(slide.getSubtitle(locale));
        r.setPrimaryCtaLabel(slide.getPrimaryCtaLabel(locale));
        r.setPrimaryCtaHref(slide.getPrimaryCtaHref());
        r.setSecondaryCtaLabel(slide.getSecondaryCtaLabel(locale));
        r.setSecondaryCtaHref(slide.getSecondaryCtaHref());
        r.setBackgroundImageUrl(buildImageUrl(slide.getBackgroundImageId()));
        return r;
    }

    private HeroSlideAdminResponse toAdminResponse(HeroSlide slide) {
        HeroSlideAdminResponse r = new HeroSlideAdminResponse();
        r.setId(slide.getId());
        r.setTitleBg(slide.getTitleBg());
        r.setTitleEn(slide.getTitleEn());
        r.setSubtitleBg(slide.getSubtitleBg());
        r.setSubtitleEn(slide.getSubtitleEn());
        r.setPrimaryCtaLabelBg(slide.getPrimaryCtaLabelBg());
        r.setPrimaryCtaLabelEn(slide.getPrimaryCtaLabelEn());
        r.setPrimaryCtaHref(slide.getPrimaryCtaHref());
        r.setSecondaryCtaLabelBg(slide.getSecondaryCtaLabelBg());
        r.setSecondaryCtaLabelEn(slide.getSecondaryCtaLabelEn());
        r.setSecondaryCtaHref(slide.getSecondaryCtaHref());
        r.setBackgroundImageId(slide.getBackgroundImageId());
        r.setBackgroundImageUrl(buildImageUrl(slide.getBackgroundImageId()));
        r.setSortOrder(slide.getSortOrder());
        r.setEnabled(slide.isEnabled());
        return r;
    }

    private String buildImageUrl(Long imageId) {
        if (imageId == null) {
            return null;
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/hero-carousel/images/")
                .path(String.valueOf(imageId))
                .toUriString();
    }

    private ResponseEntity<byte[]> imageResponse(HeroSlideImage image, String ifNoneMatch) {
        if (image.getData() == null || image.getData().length == 0) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType = MediaType.IMAGE_JPEG;
        if (image.getContentType() != null) {
            try {
                mediaType = MediaType.parseMediaType(image.getContentType());
            } catch (Exception ignored) {
                // fall back to jpeg
            }
        }
        return ImageResponses.cached(image.getData(), mediaType, ifNoneMatch);
    }
}
