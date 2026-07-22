package server.sassedo.user.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.sassedo.user.data.dto.Language;
import server.sassedo.user.data.dto.User;
import server.sassedo.user.data.projection.PublicProfileView;
import server.sassedo.user.data.projection.UserParticipantSummary;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

    /**
     * Single lightweight summary (id/name/hasPhoto) for listing-owner enrichment. Like
     * {@link #findParticipantSummariesByIdIn} it deliberately never selects the {@code profilePhoto}
     * MEDIUMBLOB, so enriching a page of listing cards does not pull owner image bytes into heap.
     */
    @Query("SELECT u.id AS id, u.name AS name, " +
            "CASE WHEN u.profilePhoto IS NOT NULL THEN true ELSE false END AS hasPhoto " +
            "FROM User u WHERE u.id = :id")
    Optional<UserParticipantSummary> findSummaryById(@Param("id") Long id);

    /**
     * Publicly shareable profile attributes for a listing owner, surfaced on the roommate detail
     * page. Deliberately selects only safe scalar fields and never the {@code profilePhoto}
     * MEDIUMBLOB; the owner's spoken languages are fetched separately via
     * {@link #findLanguagesByUserId(Long)} since they live in a join table.
     */
    @Query("SELECT u.id AS id, u.name AS name, u.age AS age, u.sex AS sex, " +
            "u.occupation AS occupation, u.smokingPreference AS smokingPreference, " +
            "u.petPolicy AS petPolicy, u.shortDescription AS shortDescription, " +
            "CASE WHEN u.profilePhoto IS NOT NULL THEN true ELSE false END AS hasPhoto " +
            "FROM User u WHERE u.id = :id")
    Optional<PublicProfileView> findPublicProfileById(@Param("id") Long id);

    @Query("SELECT l FROM User u JOIN u.languages l WHERE u.id = :id")
    List<Language> findLanguagesByUserId(@Param("id") Long id);
}
