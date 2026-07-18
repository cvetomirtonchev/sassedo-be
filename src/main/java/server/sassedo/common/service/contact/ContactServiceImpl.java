package server.sassedo.common.service.contact;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import server.sassedo.common.data.dto.ContactMessage;
import server.sassedo.common.data.dto.ContactMessageStatus;
import server.sassedo.common.data.network.request.ContactMessageRequest;
import server.sassedo.common.repository.ContactMessageRepository;
import server.sassedo.model.GenericException;
import server.sassedo.model.GenericExceptionCode;

import java.util.List;

@Service
public class ContactServiceImpl implements ContactService {

    @Autowired
    private ContactMessageRepository contactMessageRepository;

    @Override
    public ContactMessage submit(ContactMessageRequest request, String sessionEmail, String sessionName, Long userId) throws GenericException {
        String email = sessionEmail != null && !sessionEmail.isBlank() ? sessionEmail : request.getEmail();
        if (email == null || email.isBlank()) {
            throw new GenericException(GenericExceptionCode.CONTACT_EMAIL_REQUIRED, "Email is required");
        }

        String name = request.getName();
        if ((name == null || name.isBlank()) && sessionName != null && !sessionName.isBlank()) {
            name = sessionName;
        }

        ContactMessage message = new ContactMessage(
                name,
                email,
                request.getSubject(),
                request.getMessage(),
                userId
        );
        return contactMessageRepository.save(message);
    }

    @Override
    public List<ContactMessage> getAll() {
        return contactMessageRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public List<ContactMessage> markSeen(List<Long> ids) {
        List<ContactMessage> messages = contactMessageRepository.findAllById(ids);
        for (ContactMessage message : messages) {
            message.setStatus(ContactMessageStatus.SEEN);
        }
        return contactMessageRepository.saveAll(messages);
    }

    @Override
    public long countUnseen() {
        return contactMessageRepository.countByStatus(ContactMessageStatus.UNSEEN);
    }
}
