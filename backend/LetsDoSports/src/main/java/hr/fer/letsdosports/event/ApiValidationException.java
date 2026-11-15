package hr.fer.letsdosports.event;

import java.util.Map;

public class ApiValidationException extends RuntimeException {
    private final Map<String, String> errors;

    public ApiValidationException(Map<String, String> errors) {
        super("Validation failed");
        this.errors = errors;
    }

    public Map<String, String> getErrors() { return errors; }
}

