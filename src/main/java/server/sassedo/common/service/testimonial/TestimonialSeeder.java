package server.sassedo.common.service.testimonial;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import server.sassedo.common.data.dto.Testimonial;
import server.sassedo.common.repository.TestimonialRepository;

/**
 * Seeds the three default homepage testimonials on first startup when the table is empty.
 * Idempotent: skips seeding when any row already exists.
 */
@Service
@RequiredArgsConstructor
public class TestimonialSeeder {

    private final TestimonialRepository testimonialRepository;

    @PostConstruct
    public void init() {
        if (testimonialRepository.count() > 0) {
            return;
        }
        testimonialRepository.save(entry(
                "Открих квартира с ясен профил на човека отсреща и реални снимки.",
                "I found an apartment with a clear profile of the person and real photos.",
                "Анелия",
                "Aneliya",
                "Наемател",
                "Tenant"));
        testimonialRepository.save(entry(
                "Филтрите ми спестиха време, защото веднага виждам подходящите хора.",
                "The filters saved me time because I instantly see the right people.",
                "Георги",
                "Georgi",
                "Търси съквартирант",
                "Looking for a roommate"));
        testimonialRepository.save(entry(
                "Публикувах свободна стая и получих по-смислени запитвания.",
                "I posted a free room and received far more meaningful inquiries.",
                "Михаела",
                "Mihaela",
                "Предлага стая",
                "Offering a room"));
    }

    private static Testimonial entry(String quoteBg, String quoteEn, String authorBg, String authorEn,
                                     String roleBg, String roleEn) {
        Testimonial testimonial = new Testimonial();
        testimonial.setQuoteBg(quoteBg);
        testimonial.setQuoteEn(quoteEn);
        testimonial.setAuthorBg(authorBg);
        testimonial.setAuthorEn(authorEn);
        testimonial.setRoleBg(roleBg);
        testimonial.setRoleEn(roleEn);
        testimonial.setEnabled(true);
        return testimonial;
    }
}
