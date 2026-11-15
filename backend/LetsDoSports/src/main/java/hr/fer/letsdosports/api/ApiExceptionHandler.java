package hr.fer.letsdosports.api;

import hr.fer.letsdosports.user.domain.exception.InvalidUserSearchFilterException;
import hr.fer.letsdosports.user.domain.exception.UserNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(InvalidUserSearchFilterException.class)
    public ResponseEntity<ApiError> invalidFilter(InvalidUserSearchFilterException exception) {
        return ResponseEntity.badRequest().body(new ApiError("INVALID_REQUEST", exception.getMessage()));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HandlerMethodValidationException.class})
    public ResponseEntity<ApiError> invalidType() {
        return ResponseEntity.badRequest().body(new ApiError("INVALID_REQUEST", "Neispravan format parametra."));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> notFound(UserNotFoundException exception) {
        return ResponseEntity.status(404).body(new ApiError("USER_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> forbidden() {
        return ResponseEntity.status(403).body(new ApiError("ACCESS_DENIED", "Pristup nije dopušten."));
    }
}
