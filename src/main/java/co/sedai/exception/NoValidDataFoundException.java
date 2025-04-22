package co.sedai.exception;

public class NoValidDataFoundException extends RuntimeException {

    public NoValidDataFoundException(String message) {
        super(message);
    }

    public NoValidDataFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}