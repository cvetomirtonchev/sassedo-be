package server.sassedo.common.data.network.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import server.sassedo.common.data.dto.UserQuestionStatus;

@Getter
@Setter
@AllArgsConstructor
public class UserQuestionResponse {
    private Long id;
    private String name;
    private String email;
    private String childrenName;
    private String question;
    private UserQuestionStatus status;
}
