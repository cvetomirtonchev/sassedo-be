package server.sassedo.user.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.sassedo.user.data.dto.User;
import server.sassedo.user.data.projection.UserParticipantSummary;

import java.util.Collection;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.verificationCode = ?1")
    User findByVerificationCode(String code);

    User findByEmail(String email);

    @Query("SELECT u FROM User u WHERE :search IS NULL OR :search = '' " +
            "OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<User> searchUsers(@Param("search") String search, Pageable pageable);

    /**
     * Lightweight participant summaries for batch enrichment. Deliberately avoids selecting the
     * {@code profilePhoto} MEDIUMBLOB; presence is exposed as a boolean flag instead.
     */
    @Query("SELECT u.id AS id, u.name AS name, " +
            "CASE WHEN u.profilePhoto IS NOT NULL THEN true ELSE false END AS hasPhoto " +
            "FROM User u WHERE u.id IN :ids")
    List<UserParticipantSummary> findParticipantSummariesByIdIn(@Param("ids") Collection<Long> ids);
}
