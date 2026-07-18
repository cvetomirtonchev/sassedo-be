package server.sassedo.common.service.contact;

import server.sassedo.common.data.dto.ContactMessage;
import server.sassedo.common.data.network.request.ContactMessageRequest;
import server.sassedo.model.GenericException;

import java.util.List;

public interface ContactService {
    ContactMessage submit(ContactMessageRequest request, String sessionEmail, String sessionName, Long userId) throws GenericException;

    List<ContactMessage> getAll();

    List<ContactMessage> markSeen(List<Long> ids);

    long countUnseen();
}
