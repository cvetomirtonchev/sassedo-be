package server.sassedo.common.service.faq;

import server.sassedo.common.data.dto.Faq;
import server.sassedo.common.data.network.request.AddFaqRequest;
import server.sassedo.common.data.network.request.UpdateFaqRequest;
import server.sassedo.model.GenericException;

import java.util.List;

public interface FaqService {

    List<Faq> getAllOrdered();

    Faq getById(Long id) throws GenericException;

    void delete(Long id);

    Faq add(AddFaqRequest addFaqRequest);

    Faq update(UpdateFaqRequest updateFaqRequest) throws GenericException;
}
