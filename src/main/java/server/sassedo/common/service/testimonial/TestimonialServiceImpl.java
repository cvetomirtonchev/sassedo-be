package server.sassedo.common.service.testimonial;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.sassedo.common.data.dto.Testimonial;
import server.sassedo.common.data.network.request.AddTestimonialRequest;
import server.sassedo.common.data.network.request.UpdateTestimonialRequest;
import server.sassedo.common.repository.TestimonialRepository;
import server.sassedo.model.GenericException;
import server.sassedo.model.GenericExceptionCode;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TestimonialServiceImpl implements TestimonialService {

    private final TestimonialRepository testimonialRepository;

    @Override
    public List<Testimonial> getAll() {
        return testimonialRepository.findAllByOrderByIdDesc();
    }

    @Override
    public Testimonial getById(Long id) throws GenericException {
        return testimonialRepository.findById(id)
                .orElseThrow(() -> new GenericException(GenericExceptionCode.TESTIMONIAL_NOT_FOUND,
                        "Testimonial not found"));
    }

    @Override
    public List<Testimonial> randomEnabled() {
        return testimonialRepository.findRandomEnabled().stream()
                .limit(3)
                .toList();
    }

    @Override
    @Transactional
    public Testimonial add(AddTestimonialRequest request) {
        Testimonial testimonial = new Testimonial();
        testimonial.setQuoteBg(trim(request.getQuoteBg()));
        testimonial.setQuoteEn(trim(request.getQuoteEn()));
        testimonial.setAuthorBg(trim(request.getAuthorBg()));
        testimonial.setAuthorEn(trim(request.getAuthorEn()));
        testimonial.setRoleBg(trim(request.getRoleBg()));
        testimonial.setRoleEn(trim(request.getRoleEn()));
        testimonial.setEnabled(request.getEnabled() == null || request.getEnabled());
        return testimonialRepository.save(testimonial);
    }

    @Override
    @Transactional
    public Testimonial update(UpdateTestimonialRequest request) throws GenericException {
        Testimonial testimonial = getById(request.getId());
        testimonial.setQuoteBg(trim(request.getQuoteBg()));
        testimonial.setQuoteEn(trim(request.getQuoteEn()));
        testimonial.setAuthorBg(trim(request.getAuthorBg()));
        testimonial.setAuthorEn(trim(request.getAuthorEn()));
        testimonial.setRoleBg(trim(request.getRoleBg()));
        testimonial.setRoleEn(trim(request.getRoleEn()));
        testimonial.setEnabled(request.getEnabled());
        return testimonialRepository.save(testimonial);
    }

    @Override
    @Transactional
    public void delete(Long id) throws GenericException {
        Testimonial testimonial = getById(id);
        testimonialRepository.delete(testimonial);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
