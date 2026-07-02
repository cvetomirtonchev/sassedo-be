package server.sassedo.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.sassedo.common.data.dto.UserQuestion;

public interface UserQuestionsRepository extends JpaRepository<UserQuestion, Long> {

}
