package server.sassedo.user.data.dto;

import lombok.Data;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Data
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "email")
        })
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 50)
    @Email
    private String email;

    @NotBlank
    @Size(max = 120)
    private String password;

    @NotBlank
    @Size(min = 3, max = 60)
    private String name;

    @Size(max = 64)
    private String verificationCode;

    private boolean enabled;

    @ManyToMany()
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PasswordResetToken> passwordResetTokens = new ArrayList<>();

    private boolean isTermsAndConditionsAccepted;
    private boolean isGdprAccepted;

    private LocalDateTime termsAndConditionsAcceptedAt;
    private LocalDateTime gdprAcceptedAt;

    private boolean isMarketingConsentAccepted;
    private LocalDateTime marketingConsentAcceptedAt;
    private LocalDateTime marketingConsentUpdatedAt;

    public User() {
    }

    public User(String email, String password, String name, String verificationCode) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.verificationCode = verificationCode;
        this.isTermsAndConditionsAccepted = true;
        this.isGdprAccepted = true;
        this.termsAndConditionsAcceptedAt = LocalDateTime.now();
        this.gdprAcceptedAt = LocalDateTime.now();
        this.isMarketingConsentAccepted = true;
        this.marketingConsentAcceptedAt = LocalDateTime.now();
    }
}
