package server.sassedo.common.service.faq;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
    public List<Faq> getAll() {
        return faqRepository.findAll();
    }

    @Override
    public Faq getById(Long id) throws GenericException {
        return faqRepository.findById(id).orElseThrow(() -> new GenericException(GenericExceptionCode.FAQ_NOT_FOUND, "Faq not found"));
    }

    @Override
    public void delete(Long id) {
        faqRepository.deleteById(id);
    }

    @Override
    public Faq add(AddFaqRequest addFaqRequest) {
        Faq faq = new Faq(addFaqRequest.getQuestion(), addFaqRequest.getAnswer());
        return faqRepository.save(faq);
    }

    @Override
    public Faq update(UpdateFaqRequest updateFaqRequest) throws GenericException {
        Faq faq = faqRepository.findById(updateFaqRequest.getId()).orElseThrow(() -> new GenericException(GenericExceptionCode.FAQ_NOT_FOUND, "Faq not found"));
        if (updateFaqRequest.getQuestion() != null) {
            faq.setQuestion(updateFaqRequest.getQuestion());
        }
        if (updateFaqRequest.getAnswer() != null) {
            faq.setAnswer(updateFaqRequest.getAnswer());
        }
        return faqRepository.save(faq);
    }
}
