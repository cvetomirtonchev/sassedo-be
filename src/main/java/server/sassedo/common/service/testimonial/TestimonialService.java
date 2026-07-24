package server.sassedo.common.service.testimonial;

import server.sassedo.common.data.dto.Testimonial;
import server.sassedo.common.data.network.request.AddTestimonialRequest;
import server.sassedo.common.data.network.request.UpdateTestimonialRequest;
import server.sassedo.model.GenericException;

import java.util.List;

public interface TestimonialService {

    List<Testimonial> getAll();

    Testimonial getById(Long id) throws GenericException;

    List<Testimonial> randomEnabled();

    Testimonial add(AddTestimonialRequest request);

    Testimonial update(UpdateTestimonialRequest request) throws GenericException;

    void delete(Long id) throws GenericException;
}
