package server.sassedo.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import server.sassedo.common.controller.TestimonialController;
import server.sassedo.common.data.dto.Testimonial;
import server.sassedo.common.data.network.request.AddTestimonialRequest;
import server.sassedo.common.data.network.request.UpdateTestimonialRequest;
import server.sassedo.common.service.testimonial.TestimonialService;
import server.sassedo.model.GenericException;
import server.sassedo.model.GenericExceptionCode;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TestimonialControllerTest {

    private TestimonialService testimonialService;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        testimonialService = mock(TestimonialService.class);
        TestimonialController controller = new TestimonialController(testimonialService);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setValidator(validator)
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void getRandom_returnsLocalizedFieldsForBulgarianLocale() throws Exception {
        Testimonial testimonial = seededTestimonial();
        when(testimonialService.randomEnabled()).thenReturn(List.of(testimonial));

        mockMvc.perform(get("/api/testimonials/random").param("locale", "bg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quote").value(testimonial.getQuoteBg()))
                .andExpect(jsonPath("$[0].author").value(testimonial.getAuthorBg()))
                .andExpect(jsonPath("$[0].role").value(testimonial.getRoleBg()));
    }

    @Test
    void getRandom_returnsBadRequestForInvalidLocale() throws Exception {
        mockMvc.perform(get("/api/testimonials/random").param("locale", "de"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(GenericExceptionCode.FAQ_INVALID_LOCALE.name()));
    }

    @Test
    void add_rejectsIncompletePayload() throws Exception {
        AddTestimonialRequest request = new AddTestimonialRequest();
        request.setQuoteBg("only bg");

        mockMvc.perform(post("/api/testimonials/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void add_returnsAdminResponse() throws Exception {
        Testimonial saved = seededTestimonial();
        when(testimonialService.add(any(AddTestimonialRequest.class))).thenReturn(saved);

        AddTestimonialRequest request = validAddRequest();

        mockMvc.perform(post("/api/testimonials/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quoteBg").value(saved.getQuoteBg()))
                .andExpect(jsonPath("$.enabled").value(true));

        verify(testimonialService).add(any(AddTestimonialRequest.class));
    }

    @Test
    void update_returnsNotFoundErrorBody() throws Exception {
        when(testimonialService.update(any(UpdateTestimonialRequest.class)))
                .thenThrow(new GenericException(GenericExceptionCode.TESTIMONIAL_NOT_FOUND, "Testimonial not found"));

        mockMvc.perform(put("/api/testimonials/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdateRequest(404L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(GenericExceptionCode.TESTIMONIAL_NOT_FOUND.name()));
    }

    @Test
    void delete_returnsOkWhenFound() throws Exception {
        mockMvc.perform(delete("/api/testimonials/admin/7"))
                .andExpect(status().isOk());

        verify(testimonialService).delete(7L);
    }

    private static Testimonial seededTestimonial() {
        Testimonial testimonial = new Testimonial();
        testimonial.setQuoteBg("Открих квартира с ясен профил на човека отсреща и реални снимки.");
        testimonial.setQuoteEn("I found an apartment with a clear profile of the person and real photos.");
        testimonial.setAuthorBg("Анелия");
        testimonial.setAuthorEn("Aneliya");
        testimonial.setRoleBg("Наемател");
        testimonial.setRoleEn("Tenant");
        testimonial.setEnabled(true);
        return testimonial;
    }

    private static AddTestimonialRequest validAddRequest() {
        AddTestimonialRequest request = new AddTestimonialRequest();
        request.setQuoteBg("q bg");
        request.setQuoteEn("q en");
        request.setAuthorBg("a bg");
        request.setAuthorEn("a en");
        request.setRoleBg("r bg");
        request.setRoleEn("r en");
        return request;
    }

    private static UpdateTestimonialRequest validUpdateRequest(long id) {
        UpdateTestimonialRequest request = new UpdateTestimonialRequest();
        request.setId(id);
        request.setQuoteBg("q bg");
        request.setQuoteEn("q en");
        request.setAuthorBg("a bg");
        request.setAuthorEn("a en");
        request.setRoleBg("r bg");
        request.setRoleEn("r en");
        request.setEnabled(true);
        return request;
    }
}
