package server.sassedo.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.sassedo.common.data.dto.Faq;
import server.sassedo.common.data.network.request.AddFaqRequest;
import server.sassedo.common.data.network.request.UpdateFaqRequest;
import server.sassedo.common.repository.FaqRepository;
import server.sassedo.common.service.faq.FaqServiceImpl;
import server.sassedo.model.GenericException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FaqServiceImplTest {

    @Mock
    private FaqRepository faqRepository;

    @InjectMocks
    private FaqServiceImpl faqService;

    @Test
    void getAllOrdered_delegatesToOrderedQuery() {
        Faq faq = new Faq("qbg", "qen", "abg", "aen", 1);
        when(faqRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(faq));

        List<Faq> result = faqService.getAllOrdered();

        assertThat(result).containsExactly(faq);
        verify(faqRepository).findAllByOrderBySortOrderAscIdAsc();
    }

    @Test
    void add_trimsFieldsAndPersists() {
        AddFaqRequest request = new AddFaqRequest();
        request.setQuestionBg("  Въпрос  ");
        request.setQuestionEn("  Question  ");
        request.setAnswerBg("  Отговор  ");
        request.setAnswerEn("  Answer  ");
        request.setSortOrder(3);
        when(faqRepository.save(any(Faq.class))).thenAnswer(inv -> inv.getArgument(0));

        faqService.add(request);

        ArgumentCaptor<Faq> captor = ArgumentCaptor.forClass(Faq.class);
        verify(faqRepository).save(captor.capture());
        Faq saved = captor.getValue();
        assertThat(saved.getQuestionBg()).isEqualTo("Въпрос");
        assertThat(saved.getQuestionEn()).isEqualTo("Question");
        assertThat(saved.getAnswerBg()).isEqualTo("Отговор");
        assertThat(saved.getAnswerEn()).isEqualTo("Answer");
        assertThat(saved.getSortOrder()).isEqualTo(3);
    }

    @Test
    void update_appliesOnlyProvidedFields() throws GenericException {
        Faq existing = new Faq("qbg", "qen", "abg", "aen", 1);
        when(faqRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(faqRepository.save(any(Faq.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateFaqRequest request = new UpdateFaqRequest();
        request.setId(5L);
        request.setQuestionEn("  New EN  ");
        request.setSortOrder(9);

        Faq updated = faqService.update(request);

        assertThat(updated.getQuestionEn()).isEqualTo("New EN");
        assertThat(updated.getQuestionBg()).isEqualTo("qbg");
        assertThat(updated.getAnswerBg()).isEqualTo("abg");
        assertThat(updated.getSortOrder()).isEqualTo(9);
    }

    @Test
    void update_throwsWhenMissing() {
        when(faqRepository.findById(404L)).thenReturn(Optional.empty());

        UpdateFaqRequest request = new UpdateFaqRequest();
        request.setId(404L);

        assertThatThrownBy(() -> faqService.update(request))
                .isInstanceOf(GenericException.class);
    }
}
