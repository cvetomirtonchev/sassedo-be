package server.sassedo.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GenericResponse {
    private String message;

    public GenericResponse(String message) {
        super();
        this.message = message;
    }
}
