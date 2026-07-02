package server.sassedo.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.sassedo.common.data.dto.NotificationJob;

public interface NotificationJobRepository extends JpaRepository<NotificationJob, Long> {
}
