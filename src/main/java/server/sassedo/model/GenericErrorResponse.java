package server.sassedo.model;

public class GenericErrorResponse {

    private GenericExceptionCode code;
    private String message;

    public GenericErrorResponse(GenericExceptionCode code, String message) {
        super();
        this.code = code;
        this.message = message;
    }

    public GenericExceptionCode getCode() {
        return code;
    }

    public void setCode(GenericExceptionCode code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
