package server.sassedo.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.sassedo.common.data.dto.FaqLocale;
import server.sassedo.common.data.dto.Testimonial;
import server.sassedo.common.data.network.request.AddTestimonialRequest;
import server.sassedo.common.data.network.request.UpdateTestimonialRequest;
import server.sassedo.common.repository.TestimonialRepository;
import server.sassedo.common.service.testimonial.TestimonialServiceImpl;
import server.sassedo.model.GenericException;
import server.sassedo.model.GenericExceptionCode;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestimonialServiceImplTest {

    @Mock
    private TestimonialRepository testimonialRepository;

    @InjectMocks
    private TestimonialServiceImpl service;

    private static AddTestimonialRequest validAddRequest() {
        AddTestimonialRequest request = new AddTestimonialRequest();
        request.setQuoteBg("  Цитат BG  ");
        request.setQuoteEn("  Quote EN  ");
        request.setAuthorBg("  Автор  ");
        request.setAuthorEn("  Author  ");
        request.setRoleBg("  Роля  ");
        request.setRoleEn("  Role  ");
        return request;
    }

    @Test
    void randomEnabled_delegatesToRandomQueryAndCapsResultAtThree() {
        Testimonial first = new Testimonial();
        Testimonial second = new Testimonial();
        Testimonial third = new Testimonial();
        Testimonial fourth = new Testimonial();
        when(testimonialRepository.findRandomEnabled()).thenReturn(List.of(first, second, third, fourth));

        List<Testimonial> result = service.randomEnabled();

        assertThat(result).containsExactly(first, second, third);
        verify(testimonialRepository).findRandomEnabled();
    }

    @Test
    void add_trimsFieldsAndDefaultsEnabled() {
        when(testimonialRepository.save(any(Testimonial.class))).thenAnswer(inv -> inv.getArgument(0));

        service.add(validAddRequest());

        ArgumentCaptor<Testimonial> captor = ArgumentCaptor.forClass(Testimonial.class);
        verify(testimonialRepository).save(captor.capture());
        Testimonial saved = captor.getValue();
        assertThat(saved.getQuoteBg()).isEqualTo("Цитат BG");
        assertThat(saved.getQuoteEn()).isEqualTo("Quote EN");
        assertThat(saved.getAuthorBg()).isEqualTo("Автор");
        assertThat(saved.getRoleEn()).isEqualTo("Role");
        assertThat(saved.isEnabled()).isTrue();
    }

    @Test
    void add_respectsExplicitDisabledFlag() {
        AddTestimonialRequest request = validAddRequest();
        request.setEnabled(false);
        when(testimonialRepository.save(any(Testimonial.class))).thenAnswer(inv -> inv.getArgument(0));

        Testimonial saved = service.add(request);

        assertThat(saved.isEnabled()).isFalse();
    }

    @Test
    void update_replacesAllFieldsAndEnabled() throws GenericException {
        Testimonial existing = new Testimonial();
        existing.setQuoteBg("old");
        existing.setEnabled(true);
        when(testimonialRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(testimonialRepository.save(any(Testimonial.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateTestimonialRequest request = new UpdateTestimonialRequest();
        request.setId(5L);
        request.setQuoteBg("  New BG  ");
        request.setQuoteEn("  New EN  ");
        request.setAuthorBg("  A BG  ");
        request.setAuthorEn("  A EN  ");
        request.setRoleBg("  R BG  ");
        request.setRoleEn("  R EN  ");
        request.setEnabled(false);

        Testimonial updated = service.update(request);

        assertThat(updated.getQuoteBg()).isEqualTo("New BG");
        assertThat(updated.getQuoteEn()).isEqualTo("New EN");
        assertThat(updated.getAuthorBg()).isEqualTo("A BG");
        assertThat(updated.getAuthorEn()).isEqualTo("A EN");
        assertThat(updated.getRoleBg()).isEqualTo("R BG");
        assertThat(updated.getRoleEn()).isEqualTo("R EN");
        assertThat(updated.isEnabled()).isFalse();
    }

    @Test
    void update_throwsWhenMissing() {
        when(testimonialRepository.findById(404L)).thenReturn(Optional.empty());

        UpdateTestimonialRequest request = new UpdateTestimonialRequest();
        request.setId(404L);
        request.setQuoteBg("q");
        request.setQuoteEn("q");
        request.setAuthorBg("a");
        request.setAuthorEn("a");
        request.setRoleBg("r");
        request.setRoleEn("r");
        request.setEnabled(true);

        assertThatThrownBy(() -> service.update(request))
                .isInstanceOf(GenericException.class)
                .satisfies(ex -> assertThat(((GenericException) ex).getErrorResponse().getCode())
                        .isEqualTo(GenericExceptionCode.TESTIMONIAL_NOT_FOUND));
    }

    @Test
    void delete_throwsWhenMissing() {
        when(testimonialRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(9L))
                .isInstanceOf(GenericException.class)
                .satisfies(ex -> assertThat(((GenericException) ex).getErrorResponse().getCode())
                        .isEqualTo(GenericExceptionCode.TESTIMONIAL_NOT_FOUND));
    }

    @Test
    void delete_removesExistingRow() throws GenericException {
        Testimonial existing = new Testimonial();
        when(testimonialRepository.findById(3L)).thenReturn(Optional.of(existing));

        service.delete(3L);

        verify(testimonialRepository).delete(existing);
    }

    @Test
    void entityResolvesTextPerLocaleWithFallback() {
        Testimonial testimonial = new Testimonial();
        testimonial.setQuoteBg("Цитат");
        testimonial.setQuoteEn("Quote");
        testimonial.setAuthorBg("Автор");
        testimonial.setRoleEn("Role");
        assertThat(testimonial.getQuote(FaqLocale.BG)).isEqualTo("Цитат");
        assertThat(testimonial.getQuote(FaqLocale.EN)).isEqualTo("Quote");
        assertThat(testimonial.getRole(FaqLocale.EN)).isEqualTo("Role");
        assertThat(testimonial.getRole(FaqLocale.BG)).isEqualTo("Role");
    }
}
