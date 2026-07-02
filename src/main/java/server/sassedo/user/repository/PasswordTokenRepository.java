package server.sassedo.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.sassedo.user.data.dto.PasswordResetToken;

public interface PasswordTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    PasswordResetToken findTopByUserEmailOrderByExpiryDateDesc(String email);
}
