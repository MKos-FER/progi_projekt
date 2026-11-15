package hr.fer.letsdosports.user.domain.exception;

public class InvalidUserSearchFilterException extends RuntimeException {
    public InvalidUserSearchFilterException(String message) {
        super(message);
    }
}
