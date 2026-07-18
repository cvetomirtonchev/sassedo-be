package server.sassedo.common.data.dto;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "contact_messages")
@Getter
@Setter
public class ContactMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String email;

    private String subject;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    private ContactMessageStatus status;

    private Long userId;

    private LocalDateTime createdAt;

    public ContactMessage() {
    }

    public ContactMessage(String name, String email, String subject, String message, Long userId) {
        this.name = name;
        this.email = email;
        this.subject = subject;
        this.message = message;
        this.userId = userId;
        this.status = ContactMessageStatus.UNSEEN;
        this.createdAt = LocalDateTime.now();
    }
}
