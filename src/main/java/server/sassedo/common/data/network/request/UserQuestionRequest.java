package server.sassedo.common.data.network.request;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotNull;

@Getter
@Setter
public class UserQuestionRequest {
    @NotNull
    private String name;

    @NotNull
    private String email;

    private String childrenName;

    @NotNull
    private String question;
}
