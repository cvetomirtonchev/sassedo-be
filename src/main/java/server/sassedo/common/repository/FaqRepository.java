package server.sassedo.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.sassedo.common.data.dto.Faq;

import java.util.List;

public interface FaqRepository extends JpaRepository<Faq, Long> {

    List<Faq> findAllByOrderBySortOrderAscIdAsc();
}
