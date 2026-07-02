package server.sassedo.model;

public class GenericException extends Exception {
    private GenericExceptionCode code;

    public GenericException(String message) {
        super(message);
    }

    public GenericException(GenericExceptionCode code, String message) {
        super(message);
        this.code = code;
    }

    public GenericErrorResponse getErrorResponse() {
        return new GenericErrorResponse(code, getMessage());
    }
}
