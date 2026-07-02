package server.sassedo.user.data.dto;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Getter
@Setter
public class PasswordResetToken {
    public static final int EXPIRATION = 60 * 24;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String otp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "user_id")
    private User user;

    private Date expiryDate;

    private boolean isUsed;

    public PasswordResetToken() {
    }

    public PasswordResetToken(String otp, User user, Date expiryDate) {
        this.otp = otp;
        this.user = user;
        this.expiryDate = expiryDate;
        this.isUsed = false;
    }
}
