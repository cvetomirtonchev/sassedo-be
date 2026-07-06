package server.sassedo.user.data.dto;

import lombok.Data;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import server.sassedo.listing.common.PetPolicy;
import server.sassedo.listing.common.SmokerPreference;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
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

    @Size(max = 60)
    private String firstName;

    @Size(max = 60)
    private String lastName;

    @Size(max = 30)
    private String phone;

    @Lob
    @Column(columnDefinition = "MEDIUMBLOB")
    private byte[] profilePhoto;

    @Size(max = 100)
    private String city;

    private Integer age;

    @Enumerated(EnumType.STRING)
    private Sex sex;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_languages", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "language")
    private Set<Language> languages = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    private JobStatus jobStatus;

    @Size(max = 100)
    private String profession;

    @Enumerated(EnumType.STRING)
    @Column(name = "smoking_preference")
    private SmokerPreference smokingPreference;

    @Enumerated(EnumType.STRING)
    @Column(name = "pet_policy")
    private PetPolicy petPolicy;

    @Enumerated(EnumType.STRING)
    private Occupation occupation;

    @Column(length = 1000)
    private String shortDescription;

    @Size(max = 64)
    private String verificationCode;

    private boolean enabled;

    private boolean blocked;

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

    public User(String email, String password, String firstName, String lastName, String phone,
                String verificationCode, boolean acceptedTerms, boolean acceptedGdpr) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.name = buildFullName(firstName, lastName);
        this.verificationCode = verificationCode;

        LocalDateTime now = LocalDateTime.now();
        this.isTermsAndConditionsAccepted = acceptedTerms;
        this.termsAndConditionsAcceptedAt = acceptedTerms ? now : null;
        this.isGdprAccepted = acceptedGdpr;
        this.gdprAcceptedAt = acceptedGdpr ? now : null;
    }

    private static String buildFullName(String firstName, String lastName) {
        return String.join(" ",
                        firstName == null ? "" : firstName.trim(),
                        lastName == null ? "" : lastName.trim())
                .trim();
    }
}
