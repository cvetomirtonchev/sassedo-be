package server.sassedo.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.sassedo.common.data.dto.Faq;

public interface FaqRepository extends JpaRepository<Faq, Long> {
}
