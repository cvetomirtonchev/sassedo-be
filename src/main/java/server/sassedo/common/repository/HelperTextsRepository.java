package server.sassedo.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.sassedo.common.data.dto.HelperText;

public interface HelperTextsRepository extends JpaRepository<HelperText, Long> {
}
