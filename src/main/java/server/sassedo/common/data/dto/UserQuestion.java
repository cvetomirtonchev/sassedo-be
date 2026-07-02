package server.sassedo.common.data.dto;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

@Entity
@Table(name = "user_questions")
@Getter
@Setter
public class UserQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String email;

    private String childrenName;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String question;

    @Enumerated(EnumType.STRING)
    private UserQuestionStatus status;

    public UserQuestion() {
    }

    public UserQuestion(String name, String email, String childrenName, String question) {
        this.name = name;
        this.email = email;
        this.childrenName = childrenName;
        this.question = question;
        this.status = UserQuestionStatus.UNSEEN;
    }
}
