package server.sassedo.user.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.sassedo.user.data.dto.Language;
import server.sassedo.user.data.dto.User;
import server.sassedo.user.data.projection.PublicProfileView;
import server.sassedo.user.data.projection.UserEmailRecipient;
import server.sassedo.user.data.projection.UserParticipantSummary;
import server.sassedo.user.data.projection.UserReportIdentity;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u " +
            "WHERE u.email = :email AND u.deletedAt IS NULL")
    Boolean existsByEmail(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE u.verificationCode = ?1 AND u.deletedAt IS NULL")
    User findByVerificationCode(String code);

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    User findByEmail(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.deletedAt IS NULL")
    Optional<User> findActiveByIdForUpdate(@Param("id") Long id);

    @Query("SELECT u FROM User u WHERE " +
            "(:search IS NULL OR :search = '' " +
            "OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR (:searchId IS NOT NULL AND u.id = :searchId)) " +
            "AND (:cityId IS NULL OR u.preferences.preferredCity.id = :cityId)")
    Page<User> searchUsers(
            @Param("search") String search,
            @Param("searchId") Long searchId,
            @Param("cityId") Long cityId,
            Pageable pageable);

    /**
     * Lightweight participant summaries for batch enrichment. Deliberately avoids selecting the
     * {@code profilePhoto} MEDIUMBLOB; presence is exposed as a boolean flag instead.
     */
    @Query("SELECT u.id AS id, u.name AS name, " +
            "CASE WHEN u.profilePhoto IS NOT NULL THEN true ELSE false END AS hasPhoto " +
            "FROM User u WHERE u.id IN :ids AND u.deletedAt IS NULL")
    List<UserParticipantSummary> findParticipantSummariesByIdIn(@Param("ids") Collection<Long> ids);

    /**
     * Batch identity lookup for the admin report queue. Deleted users are intentionally included:
     * account deletion has already anonymized these fields, while the report remains auditable.
     */
    @Query("SELECT u.id AS id, u.name AS name, u.email AS email " +
            "FROM User u WHERE u.id IN :ids")
    List<UserReportIdentity> findReportIdentitiesByIdIn(@Param("ids") Collection<Long> ids);

    /**
     * Single lightweight summary (id/name/hasPhoto) for listing-owner enrichment. Like
     * {@link #findParticipantSummariesByIdIn} it deliberately never selects the {@code profilePhoto}
     * MEDIUMBLOB, so enriching a page of listing cards does not pull owner image bytes into heap.
     */
    @Query("SELECT u.id AS id, u.name AS name, " +
            "CASE WHEN u.profilePhoto IS NOT NULL THEN true ELSE false END AS hasPhoto " +
            "FROM User u WHERE u.id = :id AND u.deletedAt IS NULL")
    Optional<UserParticipantSummary> findSummaryById(@Param("id") Long id);

    /**
     * Name and email for transactional notifications without loading the profile-photo blob.
     */
    @Query("SELECT u.email AS email, u.name AS name FROM User u " +
            "WHERE u.id = :id AND u.deletedAt IS NULL")
    Optional<UserEmailRecipient> findEmailRecipientById(@Param("id") Long id);

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
            "FROM User u WHERE u.id = :id AND u.deletedAt IS NULL")
    Optional<PublicProfileView> findPublicProfileById(@Param("id") Long id);

    @Query("SELECT l FROM User u JOIN u.languages l WHERE u.id = :id AND u.deletedAt IS NULL")
    List<Language> findLanguagesByUserId(@Param("id") Long id);
}
