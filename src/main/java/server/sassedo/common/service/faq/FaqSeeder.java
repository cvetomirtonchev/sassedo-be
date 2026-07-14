package server.sassedo.common.service.faq;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import server.sassedo.common.data.dto.Faq;
import server.sassedo.common.repository.FaqRepository;

/**
 * Seeds the default homepage FAQ entries (Bulgarian + English) on first startup.
 * Idempotent: skips seeding if the table already contains rows.
 */
@Service
@RequiredArgsConstructor
public class FaqSeeder {

    private final FaqRepository faqRepository;

    @PostConstruct
    public void init() {
        if (faqRepository.count() > 0) {
            return;
        }
        faqRepository.save(new Faq(
                "Как работи Sassedo, за да намеря съквартирант или квартира?",
                "How does Sassedo work to help me find a roommate or an apartment?",
                "Започваш с град и сценарий. След това виждаш правилните филтри, карти и профили според избора си.",
                "You start with a city and a scenario. Then you see the right filters, cards and profiles based on your choice.",
                1));
        faqRepository.save(new Faq(
                "Безплатна ли е платформата?",
                "Is the platform free?",
                "Основното търсене и разглеждане са изградени така, че потребителят да стартира лесно. Платените възможности могат да се развиват отделно.",
                "The core search and browsing are built so that users can get started easily. Paid options can evolve separately.",
                2));
        faqRepository.save(new Faq(
                "Защо някои карти показват имот, а други човек?",
                "Why do some cards show a property and others a person?",
                "Когато търсиш квартира, основната снимка е на стаята или апартамента. Когато търсиш човек, водещата снимка идва от профила.",
                "When you search for an apartment, the main photo is of the room or the flat. When you search for a person, the lead photo comes from the profile.",
                3));
        faqRepository.save(new Faq(
                "Как да се свържа с друг потребител?",
                "How do I get in touch with another user?",
                "Избираш град и сценарий, разглеждаш подходящите резултати и се свързваш директно през платформата.",
                "You choose a city and a scenario, browse the relevant results and connect directly through the platform.",
                4));
    }
}
