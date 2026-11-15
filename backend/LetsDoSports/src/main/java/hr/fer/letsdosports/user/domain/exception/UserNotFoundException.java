package hr.fer.letsdosports.user.domain.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException() {
        super("Igrač ne postoji.");
    }
}
