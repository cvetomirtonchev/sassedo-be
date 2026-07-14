package server.sassedo.common;

import org.junit.jupiter.api.Test;
import server.sassedo.common.data.dto.Faq;
import server.sassedo.common.data.dto.FaqLocale;
import server.sassedo.model.GenericException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FaqLocaleTest {

    @Test
    void fromCode_parsesSupportedLocalesCaseInsensitively() throws GenericException {
        assertThat(FaqLocale.fromCode("bg")).isEqualTo(FaqLocale.BG);
        assertThat(FaqLocale.fromCode("EN")).isEqualTo(FaqLocale.EN);
        assertThat(FaqLocale.fromCode(" Bg ")).isEqualTo(FaqLocale.BG);
    }

    @Test
    void fromCode_rejectsUnknownAndNull() {
        assertThatThrownBy(() -> FaqLocale.fromCode("de")).isInstanceOf(GenericException.class);
        assertThatThrownBy(() -> FaqLocale.fromCode(null)).isInstanceOf(GenericException.class);
    }

    @Test
    void entityResolvesTextPerLocaleWithFallback() {
        Faq faq = new Faq("Въпрос", "Question", "Отговор", "Answer", 1);
        assertThat(faq.getQuestion(FaqLocale.BG)).isEqualTo("Въпрос");
        assertThat(faq.getQuestion(FaqLocale.EN)).isEqualTo("Question");
        assertThat(faq.getAnswer(FaqLocale.BG)).isEqualTo("Отговор");
        assertThat(faq.getAnswer(FaqLocale.EN)).isEqualTo("Answer");

        Faq bgOnly = new Faq("СамоБГ", "", "ОтговорБГ", null, 2);
        assertThat(bgOnly.getQuestion(FaqLocale.EN)).isEqualTo("СамоБГ");
        assertThat(bgOnly.getAnswer(FaqLocale.EN)).isEqualTo("ОтговорБГ");
    }
}
