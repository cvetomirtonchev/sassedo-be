package server.sassedo.common.service.faq;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.sassedo.common.data.dto.Faq;
import server.sassedo.common.data.network.request.AddFaqRequest;
import server.sassedo.common.data.network.request.UpdateFaqRequest;
import server.sassedo.common.repository.FaqRepository;
import server.sassedo.model.GenericException;
import server.sassedo.model.GenericExceptionCode;

import java.util.List;

@Service
public class FaqServiceImpl implements FaqService {
    @Autowired
    private FaqRepository faqRepository;

    @Override
    public List<Faq> getAllOrdered() {
        return faqRepository.findAllByOrderBySortOrderAscIdAsc();
    }

    @Override
    public Faq getById(Long id) throws GenericException {
        return faqRepository.findById(id)
                .orElseThrow(() -> new GenericException(GenericExceptionCode.FAQ_NOT_FOUND, "Faq not found"));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        faqRepository.deleteById(id);
    }

    @Override
    @Transactional
    public Faq add(AddFaqRequest request) {
        Faq faq = new Faq(
                request.getQuestionBg().trim(),
                request.getQuestionEn().trim(),
                request.getAnswerBg().trim(),
                request.getAnswerEn().trim(),
                request.getSortOrder());
        return faqRepository.save(faq);
    }

    @Override
    @Transactional
    public Faq update(UpdateFaqRequest request) throws GenericException {
        Faq faq = faqRepository.findById(request.getId())
                .orElseThrow(() -> new GenericException(GenericExceptionCode.FAQ_NOT_FOUND, "Faq not found"));
        if (request.getQuestionBg() != null) {
            faq.setQuestionBg(request.getQuestionBg().trim());
        }
        if (request.getQuestionEn() != null) {
            faq.setQuestionEn(request.getQuestionEn().trim());
        }
        if (request.getAnswerBg() != null) {
            faq.setAnswerBg(request.getAnswerBg().trim());
        }
        if (request.getAnswerEn() != null) {
            faq.setAnswerEn(request.getAnswerEn().trim());
        }
        if (request.getSortOrder() != null) {
            faq.setSortOrder(request.getSortOrder());
        }
        return faqRepository.save(faq);
    }
}
