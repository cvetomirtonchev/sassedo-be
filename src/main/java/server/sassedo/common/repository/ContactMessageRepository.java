package server.sassedo.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.sassedo.common.data.dto.ContactMessage;
import server.sassedo.common.data.dto.ContactMessageStatus;

import java.util.List;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
    List<ContactMessage> findAllByOrderByCreatedAtDesc();

    long countByStatus(ContactMessageStatus status);
}
